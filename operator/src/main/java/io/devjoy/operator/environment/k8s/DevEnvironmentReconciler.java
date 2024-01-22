package io.devjoy.operator.environment.k8s;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependentResource;
import io.devjoy.operator.environment.k8s.build.BuildPipelineDependentResource;
import io.devjoy.operator.environment.k8s.build.BuildPushTriggerTemplateDependentResource;
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.GiteaPushTriggerBindingDependentResource;
import io.devjoy.operator.environment.k8s.build.TriggerBindingActivationCondition;
import io.devjoy.operator.environment.k8s.build.TriggerTemplateActivationCondition;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependentResource;
import io.devjoy.operator.environment.k8s.deploy.AdditionalDeployResourcesConfigmapDependentResource;
import io.devjoy.operator.environment.k8s.deploy.ArgoActivationCondition;
import io.devjoy.operator.environment.k8s.deploy.ArgoCDDependentResource;
import io.devjoy.operator.environment.k8s.deploy.InitDeployPipelineDependentResource;
import io.devjoy.operator.environment.k8s.init.AdditionalResourceTaskDependentResource;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependentResource;
import io.devjoy.operator.environment.k8s.init.HelmCreateTaskDependentResource;
import io.devjoy.operator.environment.k8s.init.InitPipelineDependentResource;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(
	    dependents = {
	        @Dependent(activationCondition = PipelineActivationCondition.class, type = BuildPipelineDependentResource.class),
	        @Dependent(activationCondition = TriggerTemplateActivationCondition.class, type = BuildPushTriggerTemplateDependentResource.class),
	        @Dependent(activationCondition = EventListenerActivationCondition.class, type = BuildEventListenerDependentResource.class),
	        @Dependent(type = WebhookSecretDependentResource.class),
			@Dependent(type = GiteaDependentResource.class),
	        @Dependent(activationCondition = TriggerBindingActivationCondition.class, type = GiteaPushTriggerBindingDependentResource.class),
	        @Dependent(type = AdditionalResourcesConfigmapDependentResource.class),
	        @Dependent(activationCondition = PipelineActivationCondition.class, type = InitPipelineDependentResource.class),
			@Dependent(activationCondition = PipelineActivationCondition.class, type = InitDeployPipelineDependentResource.class),
			@Dependent(activationCondition = ArgoActivationCondition.class, type = ArgoCDDependentResource.class),
	        @Dependent(activationCondition = TaskActivationCondition.class, type = AdditionalResourceTaskDependentResource.class),
			@Dependent(type = AdditionalDeployResourcesConfigmapDependentResource.class),
	        @Dependent(activationCondition = TaskActivationCondition.class, type = HelmCreateTaskDependentResource.class),
	    })
public class DevEnvironmentReconciler implements Reconciler<DevEnvironment>, ErrorStatusHandler<DevEnvironment> { 
  private static final Logger LOG = LoggerFactory.getLogger(DevEnvironmentReconciler.class);
  private final KubernetesClient client;
  private final ArgoActivationCondition argoActivationCondition = new ArgoActivationCondition();
  private final PipelineActivationCondition pipelineActivationCondition = new PipelineActivationCondition();
  
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

