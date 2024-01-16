package io.devjoy.operator.environment.k8s.build;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.triggers.v1alpha1.TriggerBinding;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class TriggerBindingActivationCondition implements Condition<TriggerBinding, DevEnvironment> {
    public static String PIPELINES_API_VERSION = "v1";
    public static String PIPELINES_API_GROUP = "tekton.dev";
    @Override
    public boolean isMet(DependentResource<TriggerBinding, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
       return primary.getSpec() != null 
            && context.getClient().apiextensions().getApiGroup(PIPELINES_API_GROUP) != null
            && PIPELINES_API_VERSION.equals(context.getClient().apiextensions().getApiGroup(PIPELINES_API_GROUP).getApiVersion());
    }
}