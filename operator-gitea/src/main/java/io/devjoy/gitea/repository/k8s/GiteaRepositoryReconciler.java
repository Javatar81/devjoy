package io.devjoy.gitea.repository.k8s;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.GiteaApiService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.repository.domain.RepositoryService;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;

public class GiteaRepositoryReconciler implements Reconciler<GiteaRepository>, ErrorStatusHandler<GiteaRepository> { 
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
	public UpdateControl<GiteaRepository> reconcile(GiteaRepository resource, @SuppressWarnings("rawtypes") Context context) {
		LOG.info("Reconciling");
		if (resource.getStatus() == null) {
			resource.setStatus(new GiteaRepositoryStatus());
			resource.getStatus().setConditions(new ArrayList<>());
		}
		if (resource.getMetadata().getLabels() == null) {
			resource.getMetadata().setLabels(new HashMap<>());
		}
		Map<String, String> labels = resource.getMetadata().getLabels();
		UpdateControl<GiteaRepository> noUpdate = UpdateControl.noUpdate();
		return associatedGitea(resource, labels).map(g -> {
			LOG.info("Found Gitea {} ", g.getMetadata().getName());
			userService.getUserId(g, resource.getSpec().getUser())
				.ifPresentOrElse(id -> {},() -> userService.createUserViaExec(g, resource.getSpec().getUser()));
			
			Optional<Secret> userSecret = recocileUserSecret(resource, context, g);
			Optional<String> token = userSecret.map(s -> s.getData().get("token"))
				.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim());
			
			apiService.getBaseUri(g).ifPresent(uri -> token.ifPresent(t -> repositoryService.createIfNotExists(resource, t, uri)));
			if (!labels.containsKey(LABEL_GITEA_NAME)) {
				resource.getMetadata().getLabels().put(LABEL_GITEA_NAME,
						g.getMetadata().getName());
				resource.getMetadata().getLabels().put(LABEL_GITEA_NAMESPACE,
						g.getMetadata().getNamespace());
				return UpdateControl.updateResource(resource);
			} else {
				return noUpdate;
			}
		}).orElse(noUpdate);
		
	}

	@SuppressWarnings("unchecked")
	private Optional<Secret> recocileUserSecret(GiteaRepository resource, @SuppressWarnings("rawtypes") Context context, Gitea g) {
		Optional<Secret> userSecret = Optional.ofNullable(GiteaUserSecretDependentResource.getResource(g, resource.getSpec().getUser(), client).get());
		if (!userSecret.isPresent()) {
			LOG.info("User secret not present. Creating it for user {} ", resource.getSpec().getUser());
			GiteaUserSecretDependentResource secret = new GiteaUserSecretDependentResource(resource.getSpec().getUser(), client, tokenService);
			secret.reconcileDirectly(g, context);
		}
		return userSecret;
	}

	private Optional<Gitea> associatedGitea(GiteaRepository resource, Map<String, String> labels) {
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
}
