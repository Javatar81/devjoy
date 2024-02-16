package  io.devjoy.operator.project.k8s.deploy;

import io.argoproj.v1alpha1.Application;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ApplicationActivationCondition implements Condition<Application, Project> {
    
    @Override
    public boolean isMet(DependentResource<Application, Project> dependentResource, Project primary, Context<Project> context) {
        return serverSupportsApi(context.getClient());
    }

    public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(Application.class);
    }
}