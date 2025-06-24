package io.devjoy.gitea.k8s;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.ExtraAdminSecretDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
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
import io.devjoy.gitea.k8s.dependent.postgres.PostgresReconcileCondition;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresSecretDependent;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresServiceDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorGroupDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakOperatorReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakReconcileCondition;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakSubscriptionDependent;
import io.devjoy.gitea.k8s.domain.GiteaLabels;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.k8s.model.GiteaStatus;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.service.ServiceException;
import io.devjoy.gitea.util.UpdateControlState;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.config.informer.InformerConfiguration;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.EventSourceContext;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.EventSource;
import io.javaoperatorsdk.operator.processing.event.source.PrimaryToSecondaryMapper;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;
import io.quarkiverse.operatorsdk.annotations.RBACRule;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
import io.quarkus.runtime.util.StringUtil;
import jakarta.ws.rs.WebApplicationException;

@Workflow(dependents = { 
	@Dependent(name = "giteaConfigSecret", type = GiteaConfigSecretDependent.class),
	@Dependent(name = "giteaTrustMap", type = GiteaTrustMapDependent.class),
	@Dependent(name = "giteaDeployment", type = GiteaDeploymentDependent.class),
	@Dependent(name = "giteaAdminSecret", type = GiteaAdminSecretDependent.class),
	@Dependent(name = "keycloakClient", type = KeycloakClientDependent.class, activationCondition = KeycloakReconcileCondition.class),
	@Dependent(name = "giteaServiceAccount", type = GiteaServiceAccountDependent.class),
	@Dependent(name = "giteaService", type = GiteaServiceDependent.class), 
	@Dependent(name = "giteaRoute", type = GiteaRouteDependent.class, reconcilePrecondition = GiteaRouteReconcileCondition.class),
	@Dependent(name = "giteaPvc", type = GiteaPvcDependent.class), 
	@Dependent(name = "giteaOAuthClient", type = GiteaOAuthClientDependent.class, reconcilePrecondition = GiteaOAuthClientReconcileCondition.class),
	@Dependent(name = "postgresService", type = PostgresServiceDependent.class, reconcilePrecondition = PostgresReconcileCondition.class),
	@Dependent(name = "postgresSecret", type = PostgresSecretDependent.class, reconcilePrecondition = PostgresReconcileCondition.class), 
	@Dependent(name = "postgresPvc", type = PostgresPvcDependent.class, reconcilePrecondition = PostgresReconcileCondition.class),
	@Dependent(name = "postgresDeployment", type = PostgresDeploymentDependent.class, reconcilePrecondition = PostgresReconcileCondition.class), 
	@Dependent(name = "postgresConfig", type = PostgresConfigMapDependent.class, reconcilePrecondition = PostgresReconcileCondition.class), 
	@Dependent(name = "keycloakOG", type = KeycloakOperatorGroupDependent.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
	@Dependent(name = "keycloakSub", type = KeycloakSubscriptionDependent.class, reconcilePrecondition = KeycloakOperatorReconcileCondition.class),
	@Dependent(name = "keycloak", type = KeycloakDependent.class, activationCondition = KeycloakReconcileCondition.class), 
	@Dependent(name = "keycloakRealm", type = KeycloakRealmDependent.class, activationCondition = KeycloakReconcileCondition.class)})
@RBACRule(apiGroups = "route.openshift.io", resources = {"routes/custom-host"}, verbs = {"create","patch"})
@CSVMetadata(name = GiteaReconciler.CSV_METADATA_NAME, version = GiteaReconciler.CSV_METADATA_VERSION, displayName = "Gitea Operator", description = "An operator to manage Gitea servers and repositories", provider = @Provider(name = "devjoy.io"), keywords = "Git,Repository,Gitea", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = GiteaReconciler.CSV_CONTAINER_IMAGE, others= {}))
public class GiteaReconciler implements Reconciler<Gitea>, SharedCSVMetadata/* , EventSourceInitializer<Gitea> */{ 
	public static final String CSV_METADATA_VERSION = "0.3.0";
	public static final String CSV_METADATA_NAME = "gitea-operator-bundle.v" + CSV_METADATA_VERSION;
	public static final String CSV_CONTAINER_IMAGE = "quay.io/devjoy/gitea-operator:" + CSV_METADATA_VERSION;
	private static final Logger LOG = LoggerFactory.getLogger(GiteaReconciler.class);
	//private static final String EXTRA_ADMIN_SECRET_EVENT_SOURCE = "ExtraAdminSecretEventSource";
	private static final String EXTRA_ADMIN_SECRET_INDEX = "ExtraAdminSecretEventSourceIndex";
	//private static final String EXTRA_POSTGRES_SECRET_EVENT_SOURCE = "ExtraPostgresSecretEventSource";
	private static final String EXTRA_POSTGRES_SECRET_INDEX = "ExtraPostgresSecretEventSourceIndex";
	private final GiteaStatusUpdater updater;
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
		
		var patchableGitea = resourceForPatch(resource);
		UpdateControlState<Gitea> state = new UpdateControlState<>(patchableGitea);
		updater.init(patchableGitea);
		if (resource.getSpec() == null) {
			patchableGitea.setSpec(new GiteaSpec());
			state.patchResource();
		}
		emptyPasswordStatus(patchableGitea);
		removeAdmPwFromSpecIfInSecret(patchableGitea, context, state);
		updateHost(patchableGitea, state);
		UpdateControl<Gitea> updateCtrl = state.getState();
		if(!updateCtrl.isNoUpdate()) {
			LOG.info("Need to update ");
		} else {
			LOG.info("No update necessary");
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
		LOG.info("Checking if pw needs to be removed from spec...");
		Optional<Secret> adminSecret = context.getSecondaryResource(Secret.class, "giteaAdminSecret");
		Optional<String> adminPasswordInSecret = adminSecret.flatMap(GiteaAdminSecretDependent::getAdminPassword);
		Optional<String> adminPasswordInSpec = Optional.ofNullable(resource.getSpec())
				.map(s -> s.getAdminPassword())
				.filter(pw -> !StringUtil.isNullOrEmpty(pw));
		
		if(adminPasswordInSecret.equals(adminPasswordInSpec)) {
			LOG.info("Admin password matches the one stored in secret, hence removing it.");
			resource.getSpec().setAdminPassword(null);
			state.patchResource();
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

	private Gitea resourceForPatch(
      Gitea original) {
		var res = new Gitea();
		res.setMetadata(new ObjectMetaBuilder()
			.withName(original.getMetadata().getName())
			.withNamespace(original.getMetadata().getNamespace())
			.build());
		res.setSpec(original.getSpec());
		res.setStatus(original.getStatus());
		return res;
  	}

  	@Override
	public List<EventSource<?, Gitea>> prepareEventSources(
		EventSourceContext<Gitea> context) {
    
	context.getPrimaryCache()
		.addIndexer(EXTRA_ADMIN_SECRET_INDEX, (primary -> Optional.ofNullable(primary.getSpec() != null ? primary.getSpec().getExtraAdminSecretName() : null)
			.map(adm -> List.of(indexKey(adm, primary.getMetadata().getNamespace())))
			.orElse(Collections.emptyList())));
	context.getPrimaryCache()
		.addIndexer(EXTRA_POSTGRES_SECRET_INDEX, (primary -> Optional.ofNullable(primary.getSpec() != null ? primary.getSpec().getExtraAdminSecretName() : null)
			.map(adm -> List.of(indexKey(adm, primary.getMetadata().getNamespace())))
			.orElse(Collections.emptyList())));			
    var adminSecretEventSrc =
        new InformerEventSource<>(
            InformerEventSourceConfiguration.from(
                    Secret.class, context.getPrimaryResourceClass())
                // if there is a many-to-many relationship (thus no direct owner reference)
                // PrimaryToSecondaryMapper needs to be added
				//.with
				.withPrimaryToSecondaryMapper(
		            (PrimaryToSecondaryMapper<Gitea>) p -> 
		            Optional.ofNullable(p.getSpec() != null ? p.getSpec().getExtraAdminSecretName() : null).map(adm -> 
		            Set.of(new ResourceID(adm, p.getMetadata().getNamespace()))).orElse(Collections.emptySet()))
                .withSecondaryToPrimaryMapper(
                    s ->
                        context
                            .getPrimaryCache()
                            .byIndex(
                                EXTRA_ADMIN_SECRET_INDEX,
                                indexKey(
                                    s.getMetadata().getName(), s.getMetadata().getNamespace()))
                            .stream()
                            .map(ResourceID::fromResource)
                            .collect(Collectors.toSet()))
                .build(),
            context);
		var postgresSecretEventSrc =
			new InformerEventSource<>(
				InformerEventSourceConfiguration.from(
						Secret.class, context.getPrimaryResourceClass())
					// if there is a many-to-many relationship (thus no direct owner reference)
					// PrimaryToSecondaryMapper needs to be added
					//.with
					.withPrimaryToSecondaryMapper(
						(PrimaryToSecondaryMapper<Gitea>) p -> 
						Optional.ofNullable(p.getSpec() != null && p.getSpec().getPostgres() != null && p.getSpec().getPostgres().getUnmanagedConfig() != null ? p.getSpec().getPostgres().getUnmanagedConfig().getExtraSecretName() : null).map(adm -> 
						Set.of(new ResourceID(adm, p.getMetadata().getNamespace()))).orElse(Collections.emptySet()))
					.withSecondaryToPrimaryMapper(
						s ->
							context
								.getPrimaryCache()
								.byIndex(
									EXTRA_POSTGRES_SECRET_INDEX,
									indexKey(
										s.getMetadata().getName(), s.getMetadata().getNamespace()))
								.stream()
								.map(ResourceID::fromResource)
								.collect(Collectors.toSet()))
					.build(),
				context);
	LOG.debug("Informer source created");
	return List.of(adminSecretEventSrc, postgresSecretEventSrc);
  }

  private String indexKey(String configMapName, String namespace) {
    return configMapName + "#" + namespace;
  }
}
