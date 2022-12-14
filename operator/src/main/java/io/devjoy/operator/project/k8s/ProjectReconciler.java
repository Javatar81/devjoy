package io.devjoy.operator.project.k8s;

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
	        @Dependent(type = RepositoryDependentResource.class),
	        @Dependent(type = PipelineDependentResource.class),
	        @Dependent(type = PipelineRunDependentResource.class),
	        @Dependent(type = ConfigMapDependentResource.class),
	    })
public class ProjectReconciler implements Reconciler<Project> { 
  private final KubernetesClient client;
  private static Logger LOG = LoggerFactory.getLogger(ProjectReconciler.class);

  public ProjectReconciler(KubernetesClient client) {
    this.client = client;
  }

  // TODO Fill in the rest of the reconciler

  @Override
  public UpdateControl<Project> reconcile(Project resource, Context context) {
    // TODO: fill in logic
	LOG.info("Reconcile");
    return UpdateControl.noUpdate();
  }
}

