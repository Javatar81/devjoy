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

import io.devjoy.gitea.domain.GiteaApiService;
import io.devjoy.gitea.domain.PasswordService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.repository.domain.RepositoryService;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
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
import io.quarkus.runtime.util.StringUtil;
import jakarta.ws.rs.WebApplicationException;

public class GiteaRepositoryReconciler implements Reconciler<GiteaRepository>, ErrorStatusHandler<GiteaRepository>, Cleaner<GiteaRepository> { 
	public static final String LABEL_GITEA_NAMESPACE = "devjoy.io/gitea.namespace";
	public static final String LABEL_GITEA_NAME = "devjoy.io/gitea.name";
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
			assureUserCreated(resource, g);
			Optional<Repository> repo = assureRepositoryExists(resource, context, g);
			UpdateControl<GiteaRepository> ctrl = assureGiteaLabelsSet(resource, g);
			if (!repo.isPresent()) {
				ctrl.rescheduleAfter(10, TimeUnit.SECONDS);
			}
			return ctrl;
		}).orElse(noUpdate);
		
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
			
		},() -> {
			LOG.warn("Repository not yet present");
		});
		return repository;
	}

	private UpdateControl<GiteaRepository> assureGiteaLabelsSet(GiteaRepository resource, Gitea g) {
		LOG.info("Assure labels set");
		Map<String, String> labels = resource.getMetadata().getLabels();
		if (!labels.containsKey(LABEL_GITEA_NAME)) {
			labels.put(LABEL_GITEA_NAME,
					g.getMetadata().getName());
			labels.put(LABEL_GITEA_NAMESPACE,
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
		String giteaName = labels.get(LABEL_GITEA_NAME);
		String giteaNamespace = labels.get(LABEL_GITEA_NAMESPACE);
		if (!StringUtil.isNullOrEmpty(giteaName) && !StringUtil.isNullOrEmpty(giteaNamespace)) {
			LOG.info("Selecting gitea via label {}={}", LABEL_GITEA_NAME, giteaName);
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
				throw new IllegalArgumentException(String.format("Cannot determin unique Gitea in namespace %s", giteaNamespace));
			}
		}
	}
	
	@Override
	public ErrorStatusUpdateControl<GiteaRepository> updateErrorStatus(GiteaRepository gitea, Context<GiteaRepository> context, Exception e) {
		LOG.info("Error of type {}", e.getClass());
		if (e.getCause() instanceof ServiceException) {
			ServiceException serviceException = (ServiceException) e.getCause();
			String additionalInfo = "";
			if(serviceException.getCause() instanceof WebApplicationException) {
				WebApplicationException webException = (WebApplicationException) serviceException.getCause();
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
		return ErrorStatusUpdateControl.patchStatus(gitea);
	}

	@Override
	public DeleteControl cleanup(GiteaRepository resource, Context<GiteaRepository> context) {
		if (resource.getStatus() != null 
				&& resource.getSpec().isDeleteOnFinalize()){
			LOG.info("Deleting repository");
			associatedGitea(resource).flatMap(g -> getUserSecret(resource, g))
			.map(s ->  s.getData().get("token"))
			.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim())
			.ifPresent(t -> repositoryService.delete(resource, t));
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
