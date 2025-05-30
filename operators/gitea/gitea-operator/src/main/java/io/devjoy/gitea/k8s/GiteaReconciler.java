package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDiscriminator;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaConfigSecretDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaDeploymentDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaOAuthClientDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaOAuthClientReconcileCondition;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaPvcDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteReconcileCondition;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceAccountDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaTrustMapDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresConfigMapDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresDeploymentDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresPvcDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresSecretDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresServiceDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorGroupDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakSubscriptionDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.service.ServiceException;
import io.devjoy.gitea.util.UpdateControlState;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteSpec;
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
import io.quarkiverse.operatorsdk.annotations.RBACRule;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
import io.quarkus.runtime.util.StringUtil;
import jakarta.ws.rs.WebApplicationException;

@ControllerConfiguration(dependents = { 
		@Dependent(name = "giteaConfigSecret", type = GiteaConfigSecretDependent.class),
		@Dependent(name = "giteaTrustMap", type = GiteaTrustMapDependent.class),
		@Dependent(name = "giteaDeployment", type = GiteaDeploymentDependent.class),
		@Dependent(name = "giteaAdminSecret", type = GiteaAdminSecretDependent.class),
		@Dependent(name = "giteaServiceAccount", type = GiteaServiceAccountDependent.class),
		@Dependent(name = "giteaService", type = GiteaServiceDependent.class), 
		@Dependent(name = "giteaRoute", type = GiteaRouteDependent.class, reconcilePrecondition = GiteaRouteReconcileCondition.class),
		@Dependent(name = "giteaPvc", type = GiteaPvcDependent.class), 
		@Dependent(name = "giteaOAuthClient", type = GiteaOAuthClientDependent.class, reconcilePrecondition = GiteaOAuthClientReconcileCondition.class),
		@Dependent(name = "postgresService", type = PostgresServiceDependent.class),
		@Dependent(name = "postgresSecret", type = PostgresSecretDependent.class), 
		@Dependent(name = "postgresPvc", type = PostgresPvcDependent.class),
		@Dependent(name = "postgresDeployment", type = PostgresDeploymentDependent.class), 
		@Dependent(name = "postgresConfig", type = PostgresConfigMapDependent.class), 
		@Dependent(name = "keycloakOG", type = KeycloakOperatorGroupDependent.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
		@Dependent(name = "keycloakSub", type = KeycloakSubscriptionDependent.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
		@Dependent(name = "keycloak", type = KeycloakDependent.class, activationCondition = KeycloakReconcileCondition.class), 
		@Dependent(name = "keycloakRealm", type = KeycloakRealmDependent.class, activationCondition = KeycloakReconcileCondition.class), 
		@Dependent(name = "keycloakClient", type = KeycloakClientDependent.class, activationCondition = KeycloakReconcileCondition.class) 	
})
@RBACRule(apiGroups = "route.openshift.io", resources = {"routes/custom-host"}, verbs = {"create","patch"})
@CSVMetadata(name = GiteaReconciler.CSV_METADATA_NAME, version = GiteaReconciler.CSV_METADATA_VERSION, displayName = "Gitea Operator", description = "An operator to manage Gitea servers and repositories", provider = @Provider(name = "devjoy.io"), keywords = "Git,Repository,Gitea", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = GiteaReconciler.CSV_CONTAINER_IMAGE, others= {}))
public class GiteaReconciler implements Reconciler<Gitea>, ErrorStatusHandler<Gitea>, SharedCSVMetadata { 
	public static final String CSV_METADATA_VERSION = "0.3.0";
	public static final String CSV_METADATA_NAME = "gitea-operator-bundle.v" + CSV_METADATA_VERSION;
	public static final String CSV_CONTAINER_IMAGE = "quay.io/devjoy/gitea-operator:" + CSV_METADATA_VERSION;
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	private final GiteaStatusUpdater updater;
	private final GiteaAdminSecretDiscriminator adminSecretDiscriminator = new GiteaAdminSecretDiscriminator();
	private final OpenShiftClient client;
	public GiteaReconciler(GiteaStatusUpdater updater, OpenShiftClient client) {
		this.updater = updater;
		this.client = client;
	}

	@Override
	public UpdateControl<Gitea> reconcile(Gitea resource, Context<Gitea> context) {
		LOG.info("Reconciling gitea resource {} in namespace {}", 
			resource.getMetadata().getName(), resource.getMetadata().getNamespace());
		if(resource.getSpec() != null && "admin".equals(resource.getSpec().getAdminUser())) 
			throw new ServiceException("User 'admin' is reserved and cannot be used for spec.adminUser");
		
		UpdateControlState<Gitea> state = new UpdateControlState<>(resource);
		updater.init(resource);
		if (resource.getSpec() == null) {
			resource.setSpec(new GiteaSpec());
			state.updateResource();
		}
		emptyPasswordStatus(resource);
		removeAdmPwFromSpecIfInSecret(resource, context, state);
		updateHost(resource, state);
		UpdateControl<Gitea> updateCtrl = state.getState();
		if(!updateCtrl.isNoUpdate()) {
			LOG.info("Need to update ");
		}
		return updateCtrl;
	}

	private void updateHost(Gitea resource, UpdateControlState<Gitea> state) {
		Optional.ofNullable(GiteaRouteDependent.getResource(resource, client).get())
			.map(Route::getSpec)
			.map(RouteSpec::getHost)
			.ifPresent(h -> {
				if (!h.equals(resource.getStatus().getHost())) {
					resource.getStatus().setHost(h);
					state.patchStatus();
				}
			});
		;
	}

	private void emptyPasswordStatus(Gitea resource) {
		if (StringUtil.isNullOrEmpty(resource.getSpec().getAdminPassword())) {
			LOG.info("Password is empty. GiteaAdminSecretDependentResource will generate one.");
			resource.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(resource.getStatus().getObservedGeneration())
					.withType(GiteaConditionType.GITEA_ADMIN_PW_GENERATED.toString())
					.withMessage(String.format("Password for admin %s has been generated", resource.getSpec().getAdminUser()))
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason("No admin password given in Gitea resource.")
					.withStatus("True")
					.build()); 
		}
	}

	private void removeAdmPwFromSpecIfInSecret(Gitea resource, Context<Gitea> context,
			UpdateControlState<Gitea> state) {
		
		Optional<Secret> adminSecret = context.getSecondaryResource(Secret.class, adminSecretDiscriminator);
		Optional<String> adminPasswordInSecret = adminSecret.flatMap(GiteaAdminSecretDependent::getAdminPassword);
		Optional<String> adminPasswordInSpec = Optional.ofNullable(resource.getSpec())
				.map(s -> s.getAdminPassword())
				.filter(pw -> !StringUtil.isNullOrEmpty(pw));
		
		if(adminPasswordInSecret.equals(adminPasswordInSpec)) {
			LOG.info("Admin password matches the one stored in secret, hence removing it.");
			resource.getSpec().setAdminPassword(null);
			state.updateResource();
			resource.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(resource.getStatus().getObservedGeneration())
					.withType(GiteaConditionType.GITEA_ADMIN_PW_IN_SECRET.toString())
					.withMessage("Stored admin password in secret.")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason("New password has been provided in Gitea spec.")
					.withStatus("true")
					.build());
		}
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
					.withType(serviceException.getGiteaErrorConditionType().toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(serviceException.getMessage() + additionalInfo)
					.withStatus("false")
					.build());
		}
		return ErrorStatusUpdateControl.patchStatus(gitea);
	}
}
