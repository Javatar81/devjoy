package io.devjoy.gitea.repository.k8s;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.domain.service.PasswordService;
import io.devjoy.gitea.domain.service.ServiceException;
import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.repository.domain.RepositoryService;
import io.devjoy.gitea.repository.k8s.dependent.GiteaUserSecretDependentResource;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryConditionType;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryLabels;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryStatus;
import io.devjoy.gitea.repository.k8s.model.SecretReferenceSpec;
import io.devjoy.gitea.repository.k8s.model.WebhookSpec;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkiverse.operatorsdk.annotations.RBACRule;
import io.quarkus.runtime.util.StringUtil;
import jakarta.ws.rs.WebApplicationException;

@RBACRule(apiGroups = "apps", resources = {"replicasets"}, verbs = {"get","list","watch"})
@RBACRule(apiGroups = "", resources = {"pods"}, verbs = {"get","list","watch"})
@RBACRule(apiGroups = "", resources = {"configmaps"}, verbs = {"get","list","watch","create"})
@RBACRule(apiGroups = "", resources = {"pods/exec"}, verbs = {"get"})

public class GiteaRepositoryReconciler implements Reconciler<GiteaRepository>, ErrorStatusHandler<GiteaRepository>, Cleaner<GiteaRepository> { 
	private static final Logger LOG = LoggerFactory.getLogger(GiteaRepositoryReconciler.class);
	private final OpenShiftClient client;
	private final TokenService tokenService;
	private final PasswordService passwordService;
	private final UserService userService;
	private final RepositoryService repositoryService;
	private final GiteaApiService apiService;

	public GiteaRepositoryReconciler(OpenShiftClient client, TokenService tokenService, UserService userService,
			RepositoryService repositoryService, GiteaApiService apiService, PasswordService passwordService) {
		this.client = client;
		this.tokenService = tokenService;
		this.userService = userService;
		this.repositoryService = repositoryService;
		this.apiService = apiService;
		this.passwordService = passwordService;
	}

	@Override
	public UpdateControl<GiteaRepository> reconcile(GiteaRepository resource, Context<GiteaRepository> context) {
		LOG.info("Reconciling");
		if (resource.getStatus() == null) {
			resource.setStatus(new GiteaRepositoryStatus());
			resource.getStatus().setConditions(new ArrayList<>());
		}
		if (resource.getMetadata().getLabels() == null) {
			resource.getMetadata().setLabels(new HashMap<>());
		}
		
		UpdateControl<GiteaRepository> noUpdate = UpdateControl.noUpdate();
		noUpdate = noUpdate.rescheduleAfter(30, TimeUnit.SECONDS);
		return associatedGitea(resource).map(g -> {
			LOG.info("Found Gitea {} ", g.getMetadata().getName());
			UpdateControl<GiteaRepository> ctrl = assureGiteaLabelsSet(resource, g);
			assureUserCreated(resource, g);
			Optional<Repository> repo = assureRepositoryExists(resource, context, g);
			return ctrl.rescheduleAfter(!repo.isPresent() ? 10 : 60, TimeUnit.SECONDS);
		}).orElse(noUpdate.rescheduleAfter(10, TimeUnit.SECONDS));
		
	}

	Map<SecretReferenceSpec, String> assureWebhookSecretsExist(Stream<SecretReferenceSpec> secretRefs) {
		Map<SecretReferenceSpec, String> secrets = new HashMap<>();
		Map<String, Map<String, List<SecretReferenceSpec>>> secRefMap = secretRefs.collect(Collectors.groupingBy(SecretReferenceSpec::getNamespace, Collectors.groupingBy(SecretReferenceSpec::getName)));
		secRefMap.values().stream()
			.flatMap(refs -> refs.values().stream())
			.forEach(secRefs -> {
				Resource<Secret> secretResource = client.secrets().inNamespace(secRefs.get(0).getNamespace()).withName(secRefs.get(0).getName());
				Optional<Secret> existingSecret = Optional.ofNullable(secretResource.get());
				existingSecret.orElseThrow(() -> new ServiceException("Secret not found, referred by " + secRefs));
				existingSecret.ifPresent(s ->
					secRefs.stream().forEach(r -> {
						if (StringUtil.isNullOrEmpty(s.getData().get(r.getKey()))) {
							String generatedPassword = passwordService.generateNewPassword(12);
							s.getData().put(r.getKey(), generatedPassword);
							secrets.put(r, generatedPassword);
							secretResource
								.edit(rs -> new SecretBuilder(rs).addToData(r.getKey(), new String(Base64.getEncoder().encode(generatedPassword.getBytes())))
								.build());
						} else {
							secrets.put(r, new String(Base64.getDecoder().decode(s.getData().get(r.getKey()))));
							LOG.info("Key {} exists. Nothing to do.", r.getKey());
						}
				}));
			});
		return secrets;
	}

	private void assureUserCreated(GiteaRepository resource, Gitea g) {
		LOG.info("Assure user {} is created", resource.getSpec().getUser());
		Optional<String> userId = userService.getUserId(g, resource.getSpec().getUser())
			.map(id -> {LOG.info("User with id {} exists", id); return id;})
			.or(() -> {
				LOG.info("Creating new user");
				return userService.createUserViaExec(g, resource.getSpec().getUser());
			});
		if (userId.isEmpty()) {
			LOG.warn("Failed to create user {}", resource.getSpec().getUser());
		}
		 
	}

	private Optional<Repository> assureRepositoryExists(GiteaRepository resource, Context<GiteaRepository> context, Gitea g) {
		LOG.info("Assure repository {} is created", resource.getMetadata().getName());
		Optional<Secret> userSecret = recocileUserSecret(resource, context, g);
		Optional<String> token = userSecret.map(s -> s.getData().get("token"))
			.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim());
		
		Optional<Repository> repository = apiService.getBaseUri(g).flatMap(uri -> token.map(t -> {
			Repository repo = repositoryService.createIfNotExists(resource, t, uri);
			var secrets = Optional.ofNullable(resource.getSpec().getWebhooks()).map(w -> w.stream().map(WebhookSpec::getSecretRef)).map(this::assureWebhookSecretsExist);
			
			secrets.ifPresent(s -> repositoryService.createWebHooks(resource, s, t, uri));		
			
			return repo;
		}));
		
		LOG.info("Setting clone urls");
		repository.ifPresentOrElse(r -> {
			resource.getStatus().setCloneUrl(r.getCloneUrl());
			if (StringUtil.isNullOrEmpty(resource.getStatus().getRepositoryCreated())) {
				resource.getStatus().emitRepositoryCreated();
			}
			determineInternalCloneUrl(r.getCloneUrl(), g)
				.ifPresent(url -> resource.getStatus().setInternalCloneUrl(url));
			
		},() -> 
			LOG.warn("Repository not yet present")
		);
		return repository;
	}

	private UpdateControl<GiteaRepository> assureGiteaLabelsSet(GiteaRepository resource, Gitea g) {
		LOG.info("Assure labels set");
		Map<String, String> labels = resource.getMetadata().getLabels();
		if (!labels.containsKey(GiteaRepositoryLabels.LABEL_GITEA_NAME)) {
			LOG.info("Setting labels");
			labels.put(GiteaRepositoryLabels.LABEL_GITEA_NAME,
					g.getMetadata().getName());
			labels.put(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE,
					g.getMetadata().getNamespace());
			return UpdateControl.updateResourceAndStatus(resource);
		} else {
			return UpdateControl.updateStatus(resource);
		}
	}

	@SuppressWarnings("unchecked")
	/**
	 * We need to synchronize this method since several repositories could be reconciled in parallel.
	 * 
	 * @param resource
	 * @param context
	 * @param g
	 * @return
	 */
	private synchronized Optional<Secret> recocileUserSecret(GiteaRepository resource, @SuppressWarnings("rawtypes") Context context, Gitea g) {
		Optional<Secret> userSecret = getUserSecret(resource, g);
		if (!userSecret.isPresent()) {
			LOG.info("User secret not present. Creating it for user {} ", resource.getSpec().getUser());
		} 
		GiteaUserSecretDependentResource secret = new GiteaUserSecretDependentResource(resource.getSpec().getUser(), client, tokenService);
		secret.reconcileDirectly(g, context);
		return userSecret;
	}

	private Optional<Secret> getUserSecret(GiteaRepository resource, Gitea g) {
		return Optional.ofNullable(GiteaUserSecretDependentResource.getResource(g, resource.getSpec().getUser(), client).get());
	}

	private Optional<Gitea> associatedGitea(GiteaRepository resource) {
		LOG.info("Looking for Gitea resource");
		Map<String, String> labels = resource.getMetadata().getLabels();
		String giteaName = labels.get(GiteaRepositoryLabels.LABEL_GITEA_NAME);
		String giteaNamespace = labels.get(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE);
		if (associatedGiteaLabelsSet(resource.getMetadata())) {
			LOG.info("Selecting gitea via label {}={}", GiteaRepositoryLabels.LABEL_GITEA_NAME, giteaName);
			return Optional
					.ofNullable(client.resources(Gitea.class).inNamespace(giteaNamespace)
							.withName(giteaName).get());
		} else {
			giteaNamespace = !StringUtil.isNullOrEmpty(giteaNamespace)
					? giteaNamespace
					: resource.getMetadata().getNamespace();
			List<Gitea> giteasInSameNamespace = client.resources(Gitea.class).inNamespace(giteaNamespace).list()
					.getItems();
			if (giteasInSameNamespace.size() == 1) {
				Gitea uniqueGiteaInSameNamespace = giteasInSameNamespace.get(0);
				return Optional.of(uniqueGiteaInSameNamespace);
			} else {
				throw new GiteaNotFoundException(String.format("Cannot determine unique Gitea in namespace %s. Expected 1 but was %d.", giteaNamespace, giteasInSameNamespace.size())
				, giteaNamespace);
			}
		}
	}

	private boolean associatedGiteaLabelsSet(ObjectMeta meta) {
		return !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaRepositoryLabels.LABEL_GITEA_NAME)) 
			&& !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE));
	}
	
	@Override
	public ErrorStatusUpdateControl<GiteaRepository> updateErrorStatus(GiteaRepository gitea, Context<GiteaRepository> context, Exception e) {
		LOG.info("Error of type {}", e.getClass());
		if (e.getCause() instanceof ServiceException serviceException) {
			String additionalInfo = "";
			if(serviceException.getCause() instanceof WebApplicationException webException) {
				additionalInfo = ".Caused by http error " + webException.getResponse().getStatus();
			}
			gitea.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(gitea.getStatus().getObservedGeneration())
					.withType(serviceException.getErrorConditionType().toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(serviceException.getMessage() + additionalInfo)
					.withStatus("false")
					.build());
		}
		if (e.getCause() instanceof GiteaNotFoundException notFoundException) {
			gitea.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(gitea.getStatus().getObservedGeneration())
					.withType(GiteaRepositoryConditionType.GITEA_NOT_FOUND.toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(notFoundException.getMessage())
					.withStatus("false")
					.build());
		}
		return ErrorStatusUpdateControl.patchStatus(gitea);
	}

	@Override
	public DeleteControl cleanup(GiteaRepository resource, Context<GiteaRepository> context) {
		if (resource.getStatus() != null 
				&& resource.getSpec().isDeleteOnFinalize()){
			LOG.info("Deleting repository");
			try {
				if (associatedGiteaLabelsSet(resource.getMetadata())){
					associatedGitea(resource).flatMap(g -> getUserSecret(resource, g))
						.map(s ->  s.getData().get("token"))
						.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim())
						.filter(t -> repositoryService.getByRepo(resource, t).isPresent())
						.ifPresent(t -> repositoryService.delete(resource, t));
				} else {
					LOG.info("No labels for associated Gitea. Either it has been deleted, or you must delete it manually.");
				}
			} catch (GiteaNotFoundException e) {
				LOG.error("Skipped repository deletion because there is no associated Gitea.");
			} 
		}
		return DeleteControl.defaultDelete();
	}
	
	
	private Optional<String> determineInternalCloneUrl(String externalCloneUrl, Gitea gitea) {
		try {
			URL url = URI.create(externalCloneUrl).toURL();
			Optional<Service> giteaService = Optional
					.ofNullable(GiteaServiceDependentResource.getResource(gitea, client).get());
			return giteaService.map(s -> String.format("http://%s.%s.svc.cluster.local:%d%s", s.getMetadata().getName(),
					s.getMetadata().getNamespace(), s.getSpec().getPorts().stream()
							.filter(p -> "gitea".equals(p.getName())).map(ServicePort::getPort).findAny().orElse(80),
					url.getPath()));
		} catch (MalformedURLException e) {
			LOG.warn("No valid external clone url", e);
			return Optional.empty();
		}
	}

}