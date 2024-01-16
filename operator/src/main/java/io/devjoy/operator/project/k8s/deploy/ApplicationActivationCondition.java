package  io.devjoy.operator.project.k8s.deploy;

import io.devjoy.operator.project.k8s.Project;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ApplicationActivationCondition implements Condition<Application, Project> {
    public static String APPLICATION_API_VERSION = "v1alpha1";
    public static String ARGO_API_GROUP = "argoproj.io";
    @Override
    public boolean isMet(DependentResource<Application, Project> dependentResource, Project primary, Context<Project> context) {
       return primary.getSpec() != null 
            && context.getClient().apiextensions().getApiGroup(ARGO_API_GROUP) != null
            && APPLICATION_API_VERSION.equals(context.getClient().apiextensions().getApiGroup(ARGO_API_GROUP).getApiVersion());
    }
}