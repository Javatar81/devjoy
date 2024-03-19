package io.devjoy.operator.environment.k8s;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependent;
import io.devjoy.operator.environment.k8s.build.BuildPipelineDependent;
import io.devjoy.operator.environment.k8s.build.BuildPushTriggerTemplateDependent;
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.GiteaPushTriggerBindingDependent;
import io.devjoy.operator.environment.k8s.build.TriggerBindingActivationCondition;
import io.devjoy.operator.environment.k8s.build.TriggerTemplateActivationCondition;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependent;
import io.devjoy.operator.environment.k8s.deploy.AdditionalDeployResourcesConfigmapDependent;
import io.devjoy.operator.environment.k8s.deploy.ArgoActivationCondition;
import io.devjoy.operator.environment.k8s.deploy.ArgoCDDependentResource;
import io.devjoy.operator.environment.k8s.deploy.InitDeployPipelineDependent;
import io.devjoy.operator.environment.k8s.init.AdditionalResourceTaskDependent;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependent;
import io.devjoy.operator.environment.k8s.init.HelmCreateTaskDependent;
import io.devjoy.operator.environment.k8s.init.InitPipelineDependent;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
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
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(
	    dependents = {
	        @Dependent(activationCondition = PipelineActivationCondition.class, type = BuildPipelineDependent.class),
	        @Dependent(activationCondition = TriggerTemplateActivationCondition.class, type = BuildPushTriggerTemplateDependent.class),
	        @Dependent(activationCondition = EventListenerActivationCondition.class, type = BuildEventListenerDependent.class),
	        @Dependent(type = WebhookSecretDependent.class),
			@Dependent(type = GiteaDependentResource.class),
	        @Dependent(activationCondition = TriggerBindingActivationCondition.class, type = GiteaPushTriggerBindingDependent.class),
	        @Dependent(type = AdditionalResourcesConfigmapDependent.class),
	        @Dependent(activationCondition = PipelineActivationCondition.class, type = InitPipelineDependent.class),
			@Dependent(activationCondition = PipelineActivationCondition.class, type = InitDeployPipelineDependent.class),
			@Dependent(activationCondition = ArgoActivationCondition.class, type = ArgoCDDependentResource.class),
	        @Dependent(activationCondition = TaskActivationCondition.class, type = AdditionalResourceTaskDependent.class),
			@Dependent(type = AdditionalDeployResourcesConfigmapDependent.class),
	        @Dependent(activationCondition = TaskActivationCondition.class, type = HelmCreateTaskDependent.class),
	    })


@CSVMetadata(name = DevEnvironmentReconciler.CSV_METADATA_NAME, version = DevEnvironmentReconciler.CSV_METADATA_VERSION, displayName = "Devjoy Operator", description = "An operator to quickly create development environments and projects", provider = @Provider(name = "devjoy.io"), keywords = "Project,Quarkus,GitOps,Pipelines", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = DevEnvironmentReconciler.CSV_CONTAINER_IMAGE, others= {}))
public class DevEnvironmentReconciler implements Reconciler<DevEnvironment>, ErrorStatusHandler<DevEnvironment>, SharedCSVMetadata { 
  public static final String CSV_METADATA_VERSION = "0.2.0";
  public static final String CSV_METADATA_NAME = "devjoy-operator-bundle.v" + CSV_METADATA_VERSION;
  public static final String CSV_CONTAINER_IMAGE ="quay.io/devjoy/devjoy-operator:" + CSV_METADATA_VERSION;
  private static final Logger LOG = LoggerFactory.getLogger(DevEnvironmentReconciler.class);
  private final KubernetesClient client;
  
  public DevEnvironmentReconciler(KubernetesClient client) {
	  this.client = client;
	}

  @Override
  public UpdateControl<DevEnvironment> reconcile(DevEnvironment resource, Context<DevEnvironment> context) {
	
	LOG.info("Reconciling");
	if (resource.getStatus() == null) {
	  resource.setStatus(new DevEnvironmentStatus());
  	} 

	

	String status = resource.getStatus().toString();

	var gitea = GiteaDependentResource.getResource(client, resource).get();
	resource.getStatus().setGiteaResource(
			gitea == null ? "Error: Gitea resource not found"
					: "Gitea resource available");

	if(resource.getSpec() != null
		&& resource.getSpec().getGitea() != null 
		&& resource.getSpec().getGitea().isEnabled() 
		&& StringUtil.isNullOrEmpty(resource.getSpec().getGitea().getResourceName())
		&& gitea != null) {
		resource.getSpec().getGitea().setResourceName(gitea.getMetadata().getName());
		return UpdateControl.updateResourceAndStatus(resource);
	} else if (!status.equals(resource.getStatus().toString())) {
		return UpdateControl.patchStatus(resource);
	} else {
		UpdateControl<DevEnvironment> noUpdate = UpdateControl.noUpdate();
		return noUpdate.rescheduleAfter(30, TimeUnit.SECONDS);
	}
	
  }
  @Override
	public ErrorStatusUpdateControl<DevEnvironment> updateErrorStatus(DevEnvironment env, Context<DevEnvironment> context, Exception e) {
		LOG.info("Error of type {}", e.getClass());
		if (e.getCause() instanceof DevEnvironmentRequirementException) {
			DevEnvironmentRequirementException envNotFoundException = (DevEnvironmentRequirementException) e.getCause();
			env.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(env.getStatus().getObservedGeneration())
					.withType(DevEnvironmentConditionType.ENV_REQUIREMENT_NOT_FOUND.toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(envNotFoundException.getMessage())
					.withStatus("false")
					.build());
		}
		return ErrorStatusUpdateControl.patchStatus(env);
	}

}

