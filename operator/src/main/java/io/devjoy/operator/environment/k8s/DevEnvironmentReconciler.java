package io.devjoy.operator.environment.k8s;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependentResource;
import io.devjoy.operator.environment.k8s.build.BuildPipelineDependentResource;
import io.devjoy.operator.environment.k8s.build.BuildPushTriggerTemplateDependentResource;
import io.devjoy.operator.environment.k8s.build.GiteaPushTriggerBindingDependentResource;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependentResource;
import io.devjoy.operator.environment.k8s.deploy.AdditionalDeployResourcesConfigmapDependentResource;
import io.devjoy.operator.environment.k8s.deploy.ArgoCDDependentResource;
import io.devjoy.operator.environment.k8s.deploy.InitDeployPipelineDependentResource;
import io.devjoy.operator.environment.k8s.init.AdditionalResourceTaskDependentResource;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependentResource;
import io.devjoy.operator.environment.k8s.init.HelmCreateTaskDependentResource;
import io.devjoy.operator.environment.k8s.init.InitPipelineDependentResource;
import io.devjoy.operator.environment.k8s.init.QuarkusTaskDependentResource;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(
	    dependents = {
	        @Dependent(type = BuildPipelineDependentResource.class),
	        @Dependent(type = BuildPushTriggerTemplateDependentResource.class),
	        @Dependent(type = BuildEventListenerDependentResource.class),
	        @Dependent(type = WebhookSecretDependentResource.class),
			@Dependent(type = GiteaDependentResource.class),
	        @Dependent(type = GiteaPushTriggerBindingDependentResource.class),
	        @Dependent(type = AdditionalResourcesConfigmapDependentResource.class),
	        @Dependent(type = InitPipelineDependentResource.class),
			@Dependent(type = InitDeployPipelineDependentResource.class),
			@Dependent(type = ArgoCDDependentResource.class),
	        @Dependent(type = AdditionalResourceTaskDependentResource.class),
			@Dependent(type = AdditionalDeployResourcesConfigmapDependentResource.class),
	        @Dependent(type = HelmCreateTaskDependentResource.class),
	    })
public class DevEnvironmentReconciler implements Reconciler<DevEnvironment> { 
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

	resource.getStatus().setGiteaSubscription(
			GiteaSubscriptionDependentResource.getResource(client) == null ? "Error: Gitea subscription not found"
					: "Gitea subscription available");
	resource.getStatus().setGiteaCatalogSource(
			GiteaCatalogSourceDependentResource.getResource(client) == null ? "Error: Gitea catalog source not found"
					: "Gitea catalog source available");
	var gitea = GiteaDependentResource.getResource(client, resource).get();
	resource.getStatus().setGiteaResource(
			gitea == null ? "Error: Gitea resource not found"
					: "Gitea resource available");

	if(resource.getSpec().getGitea() != null 
		&& resource.getSpec().getGitea().isEnabled() 
		&& StringUtil.isNullOrEmpty(resource.getSpec().getGitea().getResourceName())
		&& gitea != null
		) {
		resource.getSpec().getGitea().setResourceName(gitea.getMetadata().getName());
		return UpdateControl.updateResourceAndStatus(resource);
	} else if (!status.equals(resource.getStatus().toString())) {
		return UpdateControl.patchStatus(resource);
	} else {
		UpdateControl<DevEnvironment> noUpdate = UpdateControl.noUpdate();
		return noUpdate.rescheduleAfter(30, TimeUnit.SECONDS);
	}
	
  }
}

