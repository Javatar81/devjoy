package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;

@ControllerConfiguration(
	    dependents = {
	        @Dependent(type = GiteaCatalogSourceDependentResource.class),
	        @Dependent(type = GiteaSubscriptionDependentResource.class),
	        @Dependent(type = GiteaDependentResource.class),
	        @Dependent(name = "adminSecret", type = GiteaAdminSecretDependentResource.class, reconcilePrecondition = SecretTokenNotChangedCondition.class),
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
	resource.getStatus().setGiteaSubscription(
			GiteaSubscriptionDependentResource.getResource(client) == null ? "Error: Gitea subscription not found"
					: "Gitea subscription available");
	resource.getStatus().setGiteaCatalogSource(
			GiteaCatalogSourceDependentResource.getResource(client) == null ? "Error: Gitea catalog source not found"
					: "Gitea catalog source available");
	resource.getStatus().setGiteaResource(
			GiteaDependentResource.getResource(client, resource).get() == null ? "Error: Gitea resource not found"
					: "Gitea resource available");
	resource.getStatus().setGiteaAdminSecret(
			GiteaAdminSecretDependentResource.getResource(resource, client).get() == null ? "Error: Gitea admin secret not found"
					: "Gitea admin secret available");
	return UpdateControl.patchStatus(resource);
  }
}

