package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.service.AuthenticationService;
import io.devjoy.gitea.domain.service.PasswordService;
import io.devjoy.gitea.domain.service.ServiceException;
import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaConfigSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaDeploymentDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaOAuthClientDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaOAuthClientReconcileCondition;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaPvcDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteReconcileCondition;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceAccountDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresConfigMapDependentResource;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresServiceDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorGroupDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakSubscriptionDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.devjoy.gitea.k8s.model.GiteaSpec;
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
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;
import io.quarkiverse.operatorsdk.bundle.runtime.CSVMetadataHolder.RequiredCRD;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
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
		@Dependent(type = KeycloakDependentResource.class, activationCondition = KeycloakReconcileCondition.class), 
		@Dependent(type = KeycloakRealmDependentResource.class, activationCondition = KeycloakReconcileCondition.class), 
		@Dependent(type = KeycloakClientDependentResource.class, activationCondition = KeycloakReconcileCondition.class) 
		
})
@CSVMetadata(name = GiteaReconciler.CSV_METADATA_NAME, version = GiteaReconciler.CSV_METADATA_VERSION, displayName = "Gitea Operator", description = "An operator to manage Gitea servers and repositories", provider = @Provider(name = "devjoy.io"), keywords = "Git,Repository,Gitea", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = "quay.io/devjoy/gitea-operator:0.2.0-SNAPSHOT", others= {}))
public class GiteaReconciler implements Reconciler<Gitea>, ErrorStatusHandler<Gitea>, SharedCSVMetadata { 
	public static final String CSV_METADATA_VERSION = "0.2.0";
	public static final String CSV_METADATA_NAME = "gitea-operator-bundle.v" + CSV_METADATA_VERSION;
	private static final String GITEA_TRUST_BUNDLE_MAP_NAME = "gitea-trust-bundle";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	private final OpenShiftClient client;
	private final UserService userService;
	private final AuthenticationService authService;
	private final GiteaStatusUpdater updater;
	private final PasswordService passwordService;

	public GiteaReconciler(OpenShiftClient client, UserService userService, AuthenticationService authService, GiteaStatusUpdater updater, PasswordService pwService) {
		this.client = client;
		this.userService = userService;
		this.authService = authService;
		this.updater = updater;
		this.passwordService = pwService;
	}

	@Override
	public UpdateControl<Gitea> reconcile(Gitea resource, Context<Gitea> context) {
		LOG.info("Reconciling gitea resource {} in namespace {}", 
			resource.getMetadata().getName(), resource.getMetadata().getNamespace());
		UpdateControlState<Gitea> state = new UpdateControlState<>(resource);
		updater.init(resource);
		if (resource.getSpec() == null) {
			resource.setSpec(new GiteaSpec());
			state.updateResource();
		}
		LOG.info("Waiting for Gitea pod to be ready...");
		Optional<String> adminSecretPasswordInSecret = Optional.ofNullable(GiteaAdminSecretDependentResource.getResource(resource, client).get())
				.map(s -> new String(Base64.getDecoder().decode(s.getData().get("password")))).filter(p -> !StringUtil.isNullOrEmpty(p));
		reconcileAdminUser(resource, adminSecretPasswordInSecret); 
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
			state.updateResourceAndStatus();
		} else {
			LOG.info("Admin password has not been changed.");
		}
		reconcileTrustMap(resource);
		if (resource.getSpec().isSso()) {
			reconcileAuthenticationSource(resource);
		}
		/* 
		if (client.supportsOpenShiftAPIGroup("operators.coreos.com")) {
			operatorGroupDR.reconcile(resource, context);
			subscriptionDR.reconcile(resource, context);
		}*/
		UpdateControl<Gitea> updateCtrl = state.getState();
		if(!updateCtrl.isNoUpdate()) {
			LOG.info("Need to update ");
		}
		return updateCtrl;
		
	}

	private void reconcileAdminUser(Gitea resource, Optional<String> adminSecretPasswordInSecret) {
		if("admin".equals(resource.getSpec().getAdminUser()) && adminSecretPasswordInSecret.isEmpty()) {
			throw new ServiceException("User 'admin' is reserved and cannot be used for spec.adminUser");
		} else if(!"admin".equals(resource.getSpec().getAdminUser())){
			Optional<String> adminId = userService.getAdminId(resource);
			LOG.debug("Admin id is: {} ", adminId);
			if(adminId.isEmpty()) {
				addGeneratedPasswordToSpecIfEmpty(resource);
				userService.createAdminUserViaExec(resource);
				LOG.info("Admin user created");
			} else {
				LOG.info("Admin exists");
			}
		}
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
		return ErrorStatusUpdateControl.patchStatus(gitea);
	}
}
