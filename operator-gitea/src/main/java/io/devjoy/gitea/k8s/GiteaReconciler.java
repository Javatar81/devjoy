package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.AuthenticationService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaConfigSecretDependentResource;
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
import io.devjoy.gitea.k8s.rhsso.Keycloak;
import io.devjoy.gitea.k8s.rhsso.KeycloakClient;
import io.devjoy.gitea.k8s.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakOperatorGroupDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakRealm;
import io.devjoy.gitea.k8s.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakSubscriptionDependentResource;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceInitializer;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResourceConfig;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(dependents = { @Dependent(name = "giteaConfigSecret", type = GiteaConfigSecretDependentResource.class),
		@Dependent(name = "giteaDeployment", type = GiteaDeploymentDependentResource.class),
		@Dependent(name = "giteaAdminSecret", type = GiteaAdminSecretDependentResource.class),
		//@Dependent(type = GiteaOAuthClientDependentResource.class),
		@Dependent(type = GiteaServiceAccountDependentResource.class),
		@Dependent(name = "giteaService", type = GiteaServiceDependentResource.class), 
		@Dependent(type = GiteaRouteDependentResource.class),
		@Dependent(name = "giteaPvc", type = GiteaPvcDependentResource.class), 
		@Dependent(name = "postgresService", type = PostgresServiceDependentResource.class),
		@Dependent(name = "postgresSecret", type = PostgresSecretDependentResource.class), 
		@Dependent(name = "postgresPvc", type = PostgresPvcDependentResource.class),
		@Dependent(name = "postgresDeployment", type = PostgresDeploymentDependentResource.class), 
		@Dependent(type = KeycloakOperatorGroupDependentResource.class),
		@Dependent(type = KeycloakSubscriptionDependentResource.class),
		//@Dependent(type = KeycloakClientDependentResource.class, reconcilePrecondition = KeycloakResourceReconcileCondition.class), 
		//@Dependent(type = KeycloakOperatorGroupDependentResource.class, reconcilePrecondition = KeycloakResourceReconcileCondition.class),
		//@Dependent(type = KeycloakSubscriptionDependentResource.class, reconcilePrecondition = KeycloakResourceReconcileCondition.class),
		//@Dependent(type = KeycloakDependentResource.class, reconcilePrecondition = KeycloakResourceReconcileCondition.class), 
		//@Dependent(type = KeycloakRealmDependentResource.class, reconcilePrecondition = KeycloakResourceReconcileCondition.class), 
		
})
public class GiteaReconciler implements Reconciler<Gitea>, ErrorStatusHandler<Gitea>, EventSourceInitializer<Gitea> { 
	
	private static final String GITEA_TRUST_BUNDLE_MAP_NAME = "gitea-trust-bundle";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	private final OpenShiftClient client;
	private final UserService userService;
	private final AuthenticationService authService;
	private final GiteaStatusUpdater updater;
	private KubernetesDependentResource<Keycloak, Gitea> keycloakDR;
	private KubernetesDependentResource<KeycloakClient, Gitea> keycloakClientDR;
	private KubernetesDependentResource<KeycloakRealm, Gitea> keycloakRealmDR;
	private KubernetesDependentResource<OAuthClient, Gitea> oauthClientDR;

	private static String KEYCLOAK_API_VERSION = "v1alpha1";
	
	public GiteaReconciler(OpenShiftClient client, UserService userService, AuthenticationService authService, GiteaStatusUpdater updater) {
		this.client = client;
		this.userService = userService;
		this.authService = authService;
		this.updater = updater;
		createKeycloakDependents();
	}

	private void createKeycloakDependents() {
		keycloakDR = new KeycloakDependentResource();
		keycloakDR.setKubernetesClient(client);
		keycloakDR.configureWith(new KubernetesDependentResourceConfig<>());
		keycloakClientDR = new KeycloakClientDependentResource();
		keycloakClientDR.setKubernetesClient(client);
		keycloakClientDR.configureWith(new KubernetesDependentResourceConfig<>());
		keycloakRealmDR = new KeycloakRealmDependentResource();
		keycloakRealmDR.setKubernetesClient(client);
		keycloakRealmDR.configureWith(new KubernetesDependentResourceConfig<>());
		oauthClientDR = new GiteaOAuthClientDependentResource();
		oauthClientDR.setKubernetesClient(client);
		oauthClientDR.configureWith(new KubernetesDependentResourceConfig<>());
	}

	@Override
	public Map<String, EventSource> prepareEventSources(EventSourceContext<Gitea> context) {
		return prepareKeycloakResources(context);
	}

	private Map<String, EventSource> prepareKeycloakResources(EventSourceContext<Gitea> context) {
		if(context.getClient().apiextensions().getApiGroup("keycloak.org") != null && KEYCLOAK_API_VERSION.equals(context.getClient().apiextensions().getApiGroup("keycloak.org").getApiVersion())){
			LOG.info("Keycloak is available");
			return Map.of(
				"keycloakDR", keycloakDR.initEventSource(context),
				"keycloakClientDR", keycloakClientDR.initEventSource(context),
				"keycloakRealmDR", keycloakRealmDR.initEventSource(context)
			);
		} else {
			LOG.warn("Keycloak API not available. Has it been installed on your cluster?");
			return Collections.emptyMap();
		}
	}


	@Override
	public UpdateControl<Gitea> reconcile(Gitea resource, Context<Gitea> context) {
		LOG.info("Reconciling");
		UpdateControl<Gitea> updateCtrl = UpdateControl.noUpdate();
		updater.init(resource);
		LOG.info("Waiting for Gitea pod to be ready...");
		if(!userService.getAdminId(resource).isPresent()) {
			userService.createAdminUserViaExec(resource);
			LOG.info("Admin user created");
		} else {
			LOG.info("Admin exists");
		}
		if (!StringUtil.isNullOrEmpty(resource.getSpec().getAdminPassword())) {
			String adminSecretPassword = new String(Base64.getDecoder().decode(GiteaAdminSecretDependentResource.getResource(resource, client).get().getData().get("password")));
			if (!resource.getSpec().getAdminPassword().equals(adminSecretPassword)) {
				moveAdminPasswordToSecret(resource);
				userService.changeUserPasswordViaExec(resource, resource.getSpec().getAdminUser(), resource.getSpec().getAdminPassword());
			}
			LOG.info("Removing admin password because it is stored in secret {}", GiteaAdminSecretDependentResource.getResource(resource, client).get().getMetadata().getName());
			resource.getSpec().setAdminPassword(null);
			updateCtrl = UpdateControl.updateResourceAndStatus(resource);
		} 
		reconcileTrustMap(resource);
		if (resource.getSpec().isSso()) {
			reconcileAuthenticationSource(resource);
		}
		if(!updateCtrl.isNoUpdate()) {
			LOG.info("Need to update ");
		}
		return updateCtrl;
		
	}

	private void moveAdminPasswordToSecret(Gitea resource) {
		LOG.info("New password. Moving password to secret");
		GiteaAdminSecretDependentResource.getResource(resource, client)
				.edit(s -> new SecretBuilder(s).addToData("password", new String(Base64.getEncoder().encode(resource.getSpec().getAdminPassword().getBytes())))
					.build());
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
		LOG.info("Reconciling trust map");
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

	/*@Override
	public Map<String, EventSource> prepareEventSources(EventSourceContext<Gitea> context) {
	  InformerConfiguration<Secret> config = InformerConfiguration.from(Secret.class, context).withLabelSelector(PostgresSecretDependentResource.LABEL_SELECTOR).build();
	  var configMapEventSource = new InformerEventSource<>(config, context);
	  configMapEventSource.          
      return EventSourceInitializer.nameEventSources(configMapEventSource);
	}*/
}
