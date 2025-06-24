package io.devjoy.gitea.organization.k8s;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.openapi.quarkus.gitea_json.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.domain.GiteaLabels;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.organization.k8s.dependent.GiteaAdminSecretReadonlyDependent;
import io.devjoy.gitea.organization.k8s.dependent.GiteaAdminSecretReadyPostcondition;
import io.devjoy.gitea.organization.k8s.dependent.GiteaOrganizationDependent;
import io.devjoy.gitea.organization.k8s.dependent.GiteaOrganizationOwnerDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationConditionType;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationStatus;
import io.devjoy.gitea.service.ServiceException;
import io.devjoy.gitea.util.UpdateControlState;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.AggregatedOperatorException;
import io.javaoperatorsdk.operator.api.config.informer.InformerEventSourceConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Context;
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

@Workflow(dependents = { 
	@Dependent(name = "giteaAdminSecretRo", type = GiteaAdminSecretReadonlyDependent.class, readyPostcondition = GiteaAdminSecretReadyPostcondition.class, useEventSourceWithName = OrganizationReconciler.ADMIN_SECRET_EVENT_SOURCE),
	@Dependent(name = "giteaOrganizationOwner", type = GiteaOrganizationOwnerDependent.class, dependsOn = "giteaAdminSecretRo"),
	@Dependent(name = OrganizationReconciler.DEPENDENT_NAME_GITEA_ORGANIZATION, type = GiteaOrganizationDependent.class, dependsOn = "giteaOrganizationOwner")})
public class OrganizationReconciler implements Reconciler<GiteaOrganization> {
	public static final String DEPENDENT_NAME_GITEA_ORGANIZATION = "giteaOrganization";
	public static final String ADMIN_SECRET_INDEX = "GiteaAdminSecretIndex";
	public static final String ADMIN_SECRET_EVENT_SOURCE = "GiteaAdminSecretEventSource";

	private static final Logger LOG = LoggerFactory.getLogger(OrganizationReconciler.class);
	
	@Override
	public UpdateControl<GiteaOrganization> reconcile(GiteaOrganization resource, Context<GiteaOrganization> context)
			throws Exception {
		LOG.info("Reconciling {} organization", resource.getMetadata().getName());
		var resourceToPatch = resourceForPatch(resource);
		UpdateControlState<GiteaOrganization> state = new UpdateControlState<>(resourceToPatch);
		if (resourceToPatch.getStatus() == null) {
			resourceToPatch.setStatus(new GiteaOrganizationStatus());
		}
		
		resource.associatedGitea(context.getClient()).ifPresentOrElse(g -> {
			LOG.info("Gitea found");
			assureGiteaLabelsSet(resourceToPatch, g, state);
		}, () -> {
			LOG.info("Gitea not found, reschedule after {} seconds ", 10);
			state.rescheduleAfter(10, TimeUnit.SECONDS);
		});
		
		//TODO Implement waiting on Gitea status
		
		
		context.getSecondaryResource(Organization.class).ifPresent(org -> {
			context.managedWorkflowAndDependentResourceContext().getWorkflowReconcileResult()
				.map(a -> a.getReconciledDependents().stream().filter(d -> d.resourceType() == Organization.class).findAny().orElse(null))
				//TODO.filter(r -> r.getSingleOperation().equals(Operation.CREATED))
				.ifPresent(a -> {	
					resource.getStatus().getConditions().add(new ConditionBuilder()
						.withObservedGeneration(resource.getStatus().getObservedGeneration())
						.withType(GiteaOrganizationConditionType.GITEA_ORG_CREATED.getValue())
						.withMessage("Org has been created")
						.withLastTransitionTime(LocalDateTime.now().toString())
						.withReason("Org did not exist before")
						.withStatus("true")
						.build());
					state.patchStatus();
				});
		});
		
		
		

		return state.getState();
	} 

	private void assureGiteaLabelsSet(GiteaOrganization resource, Gitea g, UpdateControlState<GiteaOrganization> state) {
		Map<String, String> labels = resource.getMetadata().getLabels();
		if (!labels.containsKey(GiteaLabels.LABEL_GITEA_NAME)) {
			LOG.debug("Setting labels");
			labels.put(GiteaLabels.LABEL_GITEA_NAME,
					g.getMetadata().getName());
			labels.put(GiteaLabels.LABEL_GITEA_NAMESPACE,
					g.getMetadata().getNamespace());
			labels.put(GiteaLabels.LABEL_GITEA_ADMIN,
					g.getSpec().getAdminConfig().getAdminUser());
			state.patchResourceAndStatus();
		} 
	}

	@Override
	public List<EventSource<?, GiteaOrganization>> prepareEventSources(
			EventSourceContext<GiteaOrganization> context) {
		// there is no owner reference in the config map, but we still want to trigger reconciliation if
   		// the config map changes. So first we add an index which custom resource references the config
    	// map.
		LOG.debug("Prepare event sources");
		context.getPrimaryCache()
			.addIndexer(ADMIN_SECRET_INDEX, (primary -> Optional.ofNullable(primary.getMetadata().getLabels().get(GiteaLabels.LABEL_GITEA_ADMIN))
				.map(adm -> List.of(indexKey(GiteaAdminSecretDependent.getName(adm), primary.getMetadata().getNamespace())))
				.orElseGet(() -> {
					LOG.warn("No secret found for index {}", ADMIN_SECRET_INDEX);
					return Collections.emptyList();
				})));
		LOG.debug("Indexer added");
		var cmES = new InformerEventSource<>(InformerEventSourceConfiguration
		        .from(Secret.class, context.getPrimaryResourceClass())
		        .withName(ADMIN_SECRET_EVENT_SOURCE)
				//.withNamespacesInheritedFromController()
				//.withNamespaces(Set.of("bschmeli-devjoy-test2"))
		        // if there is a many-to-many relationship (thus no direct owner reference)
		        // PrimaryToSecondaryMapper needs to be added
		        .withPrimaryToSecondaryMapper(
		            (PrimaryToSecondaryMapper<GiteaOrganization>) p -> 
		            	//p.associatedGitea(context.getClient()).map(g -> 
		            	//	Set.of(new ResourceID(GiteaAdminSecretDependent.getName(g), p.getMetadata().getNamespace()))).orElse(Collections.emptySet()))
		            Optional.ofNullable(p.getMetadata().getLabels().get(GiteaLabels.LABEL_GITEA_ADMIN)).map(adm -> 
		            Set.of(new ResourceID(GiteaAdminSecretDependent.getName(adm), p.getMetadata().getNamespace())))
						.orElseGet(() -> {
							LOG.warn("No secret found for index {}", ADMIN_SECRET_INDEX);
							return Collections.emptySet();
						}))
		        // the index is used to trigger reconciliation of related custom resources if secret
		        // changes
		        .withSecondaryToPrimaryMapper(cm -> context.getPrimaryCache()
					.byIndex(ADMIN_SECRET_INDEX, indexKey(cm.getMetadata().getName(),
		                cm.getMetadata().getNamespace()))
		            .stream()
					.map(ResourceID::fromResource)
					.collect(Collectors.toSet()))
		        .build(),
		        context);
		LOG.debug("Informer source created");
		return List.of(cmES);
	}
	
	private String indexKey(String secretName, String namespace) {
		if(LOG.isTraceEnabled()) {
			LOG.trace("index key {}", secretName + "#" + namespace);
		}
	    return secretName + "#" + namespace;
	}


	@Override
	public ErrorStatusUpdateControl<GiteaOrganization> updateErrorStatus(GiteaOrganization resource,
			Context<GiteaOrganization> context, Exception ex) {
		UpdateControlState<GiteaOrganization> state = new UpdateControlState<>(resource);
		if (resource.getStatus() == null) {
			resource.setStatus(new GiteaOrganizationStatus());
			state.patchStatus();
		}
		LOG.info("Error of type {}", ex.getClass());
		if(ex.getCause() instanceof AggregatedOperatorException aoe) {
			LOG.info("AggregatedOperatorException");
			aoe.getAggregatedExceptions().entrySet().stream()
				.filter(e -> e.getValue() instanceof ServiceException)
				.forEach(e -> {
					LOG.debug("Status updated");
					resource.getStatus().getConditions().add(new ConditionBuilder()
							.withObservedGeneration(resource.getStatus().getObservedGeneration())
							.withType(GiteaOrganizationConditionType.GITEA_API_ERROR.getValue())
							.withMessage(e.getValue().getMessage())
							.withLastTransitionTime(LocalDateTime.now().toString())
							.withReason("Api call failed for resource " + e.getKey())
							.withStatus("true")
							.build());
						state.patchStatus();
				});
		}
		if (state.getState().isPatchStatus()) {
			return ErrorStatusUpdateControl.patchStatus(resource);
		}
		return ErrorStatusUpdateControl.noStatusUpdate();
	}

	private GiteaOrganization resourceForPatch(
		GiteaOrganization original) {
		var res = new GiteaOrganization();
		res.setMetadata(new ObjectMetaBuilder()
			.withName(original.getMetadata().getName())
			.withNamespace(original.getMetadata().getNamespace())
			.build());
		res.setSpec(original.getSpec());
		res.setStatus(original.getStatus());
		return res;
  	}
}
