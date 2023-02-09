package io.devjoy.gitea.repository.k8s;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.GiteaApiService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.repository.domain.RepositoryService;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.runtime.util.StringUtil;

public class GiteaRepositoryReconciler implements Reconciler<GiteaRepository>, ErrorStatusHandler<GiteaRepository>, Cleaner<GiteaRepository> { 
	private static final String LABEL_GITEA_NAMESPACE = "devjoy.io/gitea.namespace";
	private static final String LABEL_GITEA_NAME = "devjoy.io/gitea.name";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaRepositoryReconciler.class);
	private final OpenShiftClient client;
	private final TokenService tokenService;
	private final UserService userService;
	private final RepositoryService repositoryService;
	private final GiteaApiService apiService;

	public GiteaRepositoryReconciler(OpenShiftClient client, TokenService tokenService, UserService userService, RepositoryService repositoryService, GiteaApiService apiService) {
		this.client = client;
		this.tokenService = tokenService;
		this.userService = userService;
		this.repositoryService = repositoryService;
		this.apiService = apiService;
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
		return associatedGitea(resource).map(g -> {
			LOG.info("Found Gitea {} ", g.getMetadata().getName());
			assureUserCreated(resource, g);
			assureRepositoryExists(resource, context, g);
			return assureGiteaLabelsSet(resource, g);
		}).orElse(noUpdate);
		
	}

	private void assureUserCreated(GiteaRepository resource, Gitea g) {
		userService.getUserId(g, resource.getSpec().getUser())
			.ifPresentOrElse(id -> {},() -> userService.createUserViaExec(g, resource.getSpec().getUser()));
	}

	private void assureRepositoryExists(GiteaRepository resource, Context<GiteaRepository> context, Gitea g) {
		Optional<Secret> userSecret = recocileUserSecret(resource, context, g);
		Optional<String> token = userSecret.map(s -> s.getData().get("token"))
			.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim());
		
		Optional<Repository> repository = apiService.getBaseUri(g).flatMap(uri -> token.map(t -> repositoryService.createIfNotExists(resource, t, uri)));
		
		LOG.info("Setting clone urls");
		repository.ifPresent(r -> {
			resource.getStatus().setCloneUrl(r.getCloneUrl());
			determineInternalCloneUrl(r.getCloneUrl(), g)
				.ifPresent(url -> resource.getStatus().setInternalCloneUrl(url));
		});
	}

	private UpdateControl<GiteaRepository> assureGiteaLabelsSet(GiteaRepository resource, Gitea g) {
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
	private Optional<Secret> recocileUserSecret(GiteaRepository resource, @SuppressWarnings("rawtypes") Context context, Gitea g) {
		Optional<Secret> userSecret = getUserSecret(resource, g);
		if (!userSecret.isPresent()) {
			LOG.info("User secret not present. Creating it for user {} ", resource.getSpec().getUser());
			GiteaUserSecretDependentResource secret = new GiteaUserSecretDependentResource(resource.getSpec().getUser(), client, tokenService);
			secret.reconcileDirectly(g, context);
		}
		return userSecret;
	}

	private Optional<Secret> getUserSecret(GiteaRepository resource, Gitea g) {
		Optional<Secret> userSecret = Optional.ofNullable(GiteaUserSecretDependentResource.getResource(g, resource.getSpec().getUser(), client).get());
		return userSecret;
	}

	private Optional<Gitea> associatedGitea(GiteaRepository resource) {
		Map<String, String> labels = resource.getMetadata().getLabels();
		if (labels.containsKey(LABEL_GITEA_NAME) && labels.containsKey(LABEL_GITEA_NAMESPACE)) {
			return Optional
					.ofNullable(client.resources(Gitea.class).inNamespace(labels.get(LABEL_GITEA_NAMESPACE))
							.withName(labels.get(LABEL_GITEA_NAME)).get());
		} else {
			String giteaNamespace = labels.containsKey(LABEL_GITEA_NAMESPACE)
					? labels.get(LABEL_GITEA_NAMESPACE)
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
		if (e instanceof ServiceException) {
			ServiceException serviceException = (ServiceException) e;
			gitea.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(gitea.getStatus().getObservedGeneration())
					.withType(serviceException.getErrorConditionType().toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(serviceException.getMessage())
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
			URL url = new URL(externalCloneUrl);
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
