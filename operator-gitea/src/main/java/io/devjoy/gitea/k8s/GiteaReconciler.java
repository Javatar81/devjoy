package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.AuthenticationService;
import io.devjoy.gitea.domain.PasswordService;
import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaConfigSecretDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaDeploymentDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaOAuthClientDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaOAuthClientReconcileCondition;
import io.devjoy.gitea.k8s.gitea.GiteaPvcDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaRouteDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaRouteReconcileCondition;
import io.devjoy.gitea.k8s.gitea.GiteaServiceAccountDependentResource;
import io.devjoy.gitea.k8s.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresConfigMapDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresSecretDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresServiceDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakOperatorGroupDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakOperatorReconcileCondition;
import io.devjoy.gitea.k8s.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.rhsso.KeycloakReconcileCondition;
import io.devjoy.gitea.k8s.rhsso.KeycloakSubscriptionDependentResource;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
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
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations.Annotation;
import io.quarkus.runtime.util.StringUtil;
import jakarta.ws.rs.WebApplicationException;

@ControllerConfiguration(dependents = { @Dependent(name = "giteaConfigSecret", type = GiteaConfigSecretDependentResource.class),
		@Dependent(name = "giteaDeployment", type = GiteaDeploymentDependentResource.class),
		@Dependent(name = "giteaAdminSecret", type = GiteaAdminSecretDependentResource.class),
		@Dependent(type = GiteaServiceAccountDependentResource.class),
		@Dependent(name = "giteaService", type = GiteaServiceDependentResource.class), 
		@Dependent(reconcilePrecondition = GiteaRouteReconcileCondition.class,name= "giteaRoute", type = GiteaRouteDependentResource.class),
		@Dependent(name = "giteaPvc", type = GiteaPvcDependentResource.class), 
		@Dependent(name = "postgresService", type = PostgresServiceDependentResource.class),
		@Dependent(name = "postgresSecret", type = PostgresSecretDependentResource.class), 
		@Dependent(name = "postgresPvc", type = PostgresPvcDependentResource.class),
		@Dependent(name = "postgresDeployment", type = PostgresDeploymentDependentResource.class), 
		@Dependent(name = "postgresConfig", type = PostgresConfigMapDependentResource.class), 
		@Dependent(reconcilePrecondition = GiteaOAuthClientReconcileCondition.class, type = GiteaOAuthClientDependentResource.class),
		@Dependent(name = "keycloakOG", type = KeycloakOperatorGroupDependentResource.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
		@Dependent(name = "keycloakSub", type = KeycloakSubscriptionDependentResource.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
		@Dependent(type = KeycloakDependentResource.class, reconcilePrecondition = KeycloakReconcileCondition.class), 
		@Dependent(type = KeycloakRealmDependentResource.class, reconcilePrecondition = KeycloakReconcileCondition.class), 
		@Dependent(type = KeycloakClientDependentResource.class, reconcilePrecondition = KeycloakReconcileCondition.class) 
		
})
@CSVMetadata(name = "gitea-operator-bundle", version = "0.1.0", displayName = "Gitea Operator", description = "An operator to manage Gitea servers and repositories", provider = @Provider(name = "devjoy.io"), keywords = "Git,Repository,Gitea", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = "quay.io/devjoy/gitea-operator:0.1.0", others= {}))
public class GiteaReconciler implements Reconciler<Gitea>, ErrorStatusHandler<Gitea>, EventSourceInitializer<Gitea>, SharedCSVMetadata { 
	
	private static final String GITEA_TRUST_BUNDLE_MAP_NAME = "gitea-trust-bundle";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	private final OpenShiftClient client;
	private final UserService userService;
	private final AuthenticationService authService;
	private final GiteaStatusUpdater updater;
	private final PasswordService passwordService;
	/*private KubernetesDependentResource<Keycloak, Gitea> keycloakDR;
	private KubernetesDependentResource<KeycloakClient, Gitea> keycloakClientDR;
	private KubernetesDependentResource<KeycloakRealm, Gitea> keycloakRealmDR;
	private KubernetesDependentResource<OperatorGroup, Gitea> operatorGroupDR;
	private KubernetesDependentResource<Subscription, Gitea> subscriptionDR;
	private KubernetesDependentResource<OAuthClient, Gitea> oauthClientDR;
	private KubernetesDependentResource<Route, Gitea> giteaRouteDR;	*/

	private static String KEYCLOAK_API_VERSION = "v1alpha1";
	
	public GiteaReconciler(OpenShiftClient client, UserService userService, AuthenticationService authService, GiteaStatusUpdater updater, PasswordService pwService) {
		this.client = client;
		this.userService = userService;
		this.authService = authService;
		this.updater = updater;
		this.passwordService = pwService;
		createKeycloakDependents();
	}

	private void createKeycloakDependents() {
		/*keycloakDR = new KeycloakDependentResource();
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
		giteaRouteDR = new GiteaRouteDependentResource();
		giteaRouteDR.setKubernetesClient(client);
		giteaRouteDR.configureWith(new KubernetesDependentResourceConfig<>());
		operatorGroupDR = new KeycloakOperatorGroupDependentResource();
		operatorGroupDR.setKubernetesClient(client);
		operatorGroupDR.configureWith(new KubernetesDependentResourceConfig<>());
		subscriptionDR = new KeycloakSubscriptionDependentResource();
		subscriptionDR.setKubernetesClient(client);
		subscriptionDR.configureWith(new KubernetesDependentResourceConfig<>());*/
	}

	@Override
	public Map<String, EventSource> prepareEventSources(EventSourceContext<Gitea> context) {
		Map<String, EventSource> eventSources = new HashMap<>();
		/*if(keycloakApiAvailable()){
			LOG.info("Keycloak is available");
			eventSources.put("keycloakDR", keycloakDR.initEventSource(context));
			eventSources.put("keycloakClientDR", keycloakClientDR.initEventSource(context));
			eventSources.put("keycloakRealmDR", keycloakRealmDR.initEventSource(context));
		} else {
			LOG.warn("Keycloak API not available. Has it been installed on your cluster?");
		}*/
		//if (client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)) {
		//	eventSources.put("giteaRouteDR", giteaRouteDR.initEventSource(context));
		//} else {
		//	LOG.warn("OpenShift API Group {} not available. Route cannot be created", OpenShiftAPIGroups.ROUTE);
		//}
		/*if (client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.OAUTH)) {
			eventSources.put("oauthClientDR", oauthClientDR.initEventSource(context));
		} else {
			LOG.warn("OpenShift API Group {} not available. No oauth integration into RHSSO possible", OpenShiftAPIGroups.OAUTH);
		}
		if (client.supportsOpenShiftAPIGroup("operators.coreos.com")) {
			eventSources.put("operatorGroupDR", operatorGroupDR.initEventSource(context));
			eventSources.put("subscriptionDR", subscriptionDR.initEventSource(context));
		}
		else {
			LOG.warn("OpenShift API Group operators.coreos.com not available. RHSSO must be installed manually.");
		}*/
		return eventSources;
	}
	

	private boolean keycloakApiAvailable() {
		return client.apiextensions().getApiGroup("keycloak.org") != null 
			&& KEYCLOAK_API_VERSION.equals(client.apiextensions().getApiGroup("keycloak.org").getApiVersion());
	}


	private void addGeneratedPasswordToSpecIfEmpty(Gitea gitea) {
		if (StringUtil.isNullOrEmpty(gitea.getSpec().getAdminPassword())) {
			LOG.info("Admin password is empty. Generating new one.");
			int length = gitea.getSpec().getAdminPasswordLength() < 10 ? 10 : gitea.getSpec().getAdminPasswordLength();
			gitea.getSpec().setAdminPassword(passwordService.generateNewPassword(length));  
			gitea.getSpec().setAdminPasswordLength(length);
			gitea.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(gitea.getStatus().getObservedGeneration())
					.withType(GiteaConditionType.GITEA_ADMIN_PW_GENERATED.toString())
					.withMessage(String.format("Password for admin %s has been generated", gitea.getSpec().getAdminUser()))
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason("No admin password given in Gitea resource.")
					.withStatus("True")
					.build()); 
		}
	}

	@Override
	public UpdateControl<Gitea> reconcile(Gitea resource, Context<Gitea> context) {
		LOG.info("Reconciling");
		UpdateControl<Gitea> updateCtrl = UpdateControl.noUpdate();
		updater.init(resource);
		//if(resource.getSpec().isIngressEnabled() && client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)){
		//	LOG.info("Route is enabled");
		//	giteaRouteDR.reconcile(resource, context);
		//} else {
		//	LOG.info("Route is disabled");
		//}
		LOG.info("Waiting for Gitea pod to be ready...");
		Optional<String> adminSecretPasswordInSecret = Optional.ofNullable(GiteaAdminSecretDependentResource.getResource(resource, client).get())
				.map(s -> new String(Base64.getDecoder().decode(s.getData().get("password")))).filter(p -> !StringUtil.isNullOrEmpty(p));
		//admin user exists by default, no need to create
		if("admin".equals(resource.getSpec().getAdminUser()) && adminSecretPasswordInSecret.isEmpty()) {
			//addGeneratedPasswordToSpecIfEmpty(resource);
			//LOG.info("Default admin exists. Only need to change the password.");
			//userService.changeUserPasswordViaExec(resource, resource.getSpec().getAdminUser(), resource.getSpec().getAdminPassword());
			throw new ServiceException("User 'admin' is reserved and cannot be used for spec.adminUser");
		} else if(!"admin".equals(resource.getSpec().getAdminUser())){
			if(!userService.getAdminId(resource).isPresent()) {
				addGeneratedPasswordToSpecIfEmpty(resource);
				userService.createAdminUserViaExec(resource);
				LOG.info("Admin user created");
			} else {
				LOG.info("Admin exists");
			}
		} 
		
		if (!StringUtil.isNullOrEmpty(resource.getSpec().getAdminPassword())) {
			
			if (adminSecretPasswordInSecret.isEmpty() || !resource.getSpec().getAdminPassword().equals(adminSecretPasswordInSecret.get())) {
				LOG.info("Updating admin password in secret");
				moveAdminPasswordToSecret(resource);
			} 
			if (adminSecretPasswordInSecret.isPresent() && !resource.getSpec().getAdminPassword().equals(adminSecretPasswordInSecret.get())) {
				LOG.info("Admin password changed. ");
				userService.changeUserPasswordViaExec(resource, resource.getSpec().getAdminUser(), resource.getSpec().getAdminPassword());
			}
			
			Optional<Secret> adminSecretResource = Optional.ofNullable(GiteaAdminSecretDependentResource.getResource(resource, client).get());
			adminSecretResource.ifPresent(r -> LOG.info("Removing admin password because it is stored in secret {}", r.getMetadata().getName()));
			resource.getSpec().setAdminPassword(null);
			updateCtrl = UpdateControl.updateResourceAndStatus(resource);
		} else {
			LOG.info("Admin password has not been changed.");
		}
		reconcileTrustMap(resource);
		/*if(keycloakApiAvailable()){
			reconcileSsoResources(resource, context);
		}*/
		
		if (resource.getSpec().isSso()) {
			reconcileAuthenticationSource(resource);
		}
		/*if (client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.OAUTH)) {
			oauthClientDR.reconcile(resource, context);
		}
		if (client.supportsOpenShiftAPIGroup("operators.coreos.com")) {
			operatorGroupDR.reconcile(resource, context);
			subscriptionDR.reconcile(resource, context);
		}*/
		if(!updateCtrl.isNoUpdate()) {
			LOG.info("Need to update ");
		}
		return updateCtrl;
		
	}

	/*public void reconcileSsoResources(Gitea primary, Context<Gitea> context) {
		keycloakDR.reconcile(primary, context);
    	keycloakClientDR.reconcile(primary, context);
    	keycloakRealmDR.reconcile(primary, context);
		oauthClientDR.reconcile(primary, context);
	}*/

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

	/*@Override
	public Map<String, EventSource> prepareEventSources(EventSourceContext<Gitea> context) {
	  InformerConfiguration<Secret> config = InformerConfiguration.from(Secret.class, context).withLabelSelector(PostgresSecretDependentResource.LABEL_SELECTOR).build();
	  var configMapEventSource = new InformerEventSource<>(config, context);
	  configMapEventSource.          
      return EventSourceInitializer.nameEventSources(configMapEventSource);
	}*/
}
