package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.AuthenticationService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaConfigMapDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaDeploymentDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaOAuthClientDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaPvcDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaRouteDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaServiceAccountDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresSecretDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresServiceDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakOperatorGroupDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakSubscriptionDependentResource;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.utils.Base64;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(dependents = { @Dependent(type = GiteaConfigMapDependentResource.class),
		@Dependent(type = GiteaDeploymentDependentResource.class),
		@Dependent(type = GiteaAdminSecretDependentResource.class),
		@Dependent(type = GiteaOAuthClientDependentResource.class),
		@Dependent(type = GiteaServiceAccountDependentResource.class),
		@Dependent(type = GiteaServiceDependentResource.class), @Dependent(type = GiteaRouteDependentResource.class),
		@Dependent(type = GiteaPvcDependentResource.class), @Dependent(type = PostgresServiceDependentResource.class),
		@Dependent(type = PostgresSecretDependentResource.class), @Dependent(type = PostgresPvcDependentResource.class),
		@Dependent(type = PostgresDeploymentDependentResource.class), 
		@Dependent(type = KeycloakOperatorGroupDependentResource.class),
		@Dependent(type = KeycloakSubscriptionDependentResource.class),
		@Dependent(type = KeycloakDependentResource.class), 
		@Dependent(type = KeycloakRealmDependentResource.class), 
		@Dependent(type = KeycloakClientDependentResource.class), 
})
public class GiteaReconciler implements Reconciler<Gitea>, ErrorStatusHandler<Gitea> { 
	
	private static final String GITEA_TRUST_BUNDLE_MAP_NAME = "gitea-trust-bundle";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	private final OpenShiftClient client;
	private final UserService userService;
	private final AuthenticationService authService;
	private final GiteaStatusUpdater updater;
	
	public GiteaReconciler(OpenShiftClient client, UserService userService, AuthenticationService authService, GiteaStatusUpdater updater) {
		this.client = client;
		this.userService = userService;
		this.authService = authService;
		this.updater = updater;
	}

	@Override
	public UpdateControl<Gitea> reconcile(Gitea resource, Context<Gitea> context) {
		LOG.info("reconciling");
		UpdateControl<Gitea> updateCtrl = UpdateControl.updateStatus(resource);
		updater.init(resource);
		if(!userService.getAdminId(resource).isPresent()) {
			userService.createAdminUserViaExec(resource);
			LOG.info("Admin user created");
		} else {
			LOG.info("Admin exists");
		}
		if (!StringUtil.isNullOrEmpty(resource.getSpec().getAdminPassword())) {
			moveAdminPasswordToSecret(resource);
			updateCtrl = UpdateControl.updateResourceAndStatus(resource);
		} 
		reconcileTrustMap(resource);
		reconcileAuthenticationSource(resource);
		return updateCtrl;
		
	}

	private void moveAdminPasswordToSecret(Gitea resource) {
		LOG.info("New password. Moving {} password to secret", resource.getSpec().getAdminPassword());
		GiteaAdminSecretDependentResource.getResource(resource, client)
				.edit(s -> new SecretBuilder(s).addToData("password", Base64.encodeBytes(resource.getSpec().getAdminPassword().getBytes()))
					.build());
		resource.getSpec().setAdminPassword(null);
		resource.getStatus().getConditions().add(new ConditionBuilder()
				.withObservedGeneration(resource.getStatus().getObservedGeneration())
				.withType(GiteaConditionType.GITEA_ADMIN_PW_IN_SECRET.toString())
				.withMessage("Stored admin password in secret.")
				.withLastTransitionTime(LocalDateTime.now().toString())
				.withReason("New password has been provided in Gitea spec.")
				.withStatus("true")
				.build());
		LOG.info("Done, updating status.");
	}

	private ConfigMap reconcileTrustMap(Gitea resource) {
		LOG.info("reconciling trust map");
		Optional<ConfigMap> trustMap = Optional.ofNullable(client.configMaps().inNamespace(resource.getMetadata().getNamespace()).withName(GITEA_TRUST_BUNDLE_MAP_NAME).get());
		return trustMap.orElseGet(() -> {
			LOG.info("Creating trust map");
			ConfigMap newTrustMap = new ConfigMapBuilder()
					.withNewMetadata()
						.withName(GITEA_TRUST_BUNDLE_MAP_NAME)
						.withOwnerReferences(new OwnerReferenceBuilder()
								.withUid(resource.getMetadata().getUid())
								.withKind(resource.getKind())
								.withApiVersion(resource.getApiVersion())
								.withName(resource.getMetadata().getName())
								.build())
						.addToLabels("config.openshift.io/inject-trusted-cabundle", "true")
						.addToLabels("devjoy.io/cm.role", "trustmap")
						//.addToAnnotations("service.beta.openshift.io/inject-cabundle", "true")
					.endMetadata()
					.build();
			client.configMaps().inNamespace(resource.getMetadata().getNamespace()).create(newTrustMap);
			return newTrustMap;
		});
	}

	private void reconcileAuthenticationSource(Gitea resource) {
		LOG.info("reconcile authentication");
		authService.getAuthenticationSourceId(resource).ifPresentOrElse(id -> 
			authService.updateAuthenticationSource(resource, id)
		, () -> 
			authService.registerAuthenticationSource(resource)
		);
	}

	@Override
	public ErrorStatusUpdateControl<Gitea> updateErrorStatus(Gitea gitea, Context<Gitea> context, Exception e) {
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
