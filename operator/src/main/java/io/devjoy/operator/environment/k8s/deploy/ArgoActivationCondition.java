package io.devjoy.operator.environment.k8s.deploy;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ArgoActivationCondition implements Condition<ArgoCD, DevEnvironment> {
    public static String APPLICATION_API_VERSION = "v1alpha1";
    public static String ARGO_API_GROUP = "argoproj.io";
    @Override
    public boolean isMet(DependentResource<ArgoCD, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
       return isArgoApiAvailable(context.getClient());
    }

    public static boolean isArgoApiAvailable(KubernetesClient client) {
        return client.apiextensions().getApiGroup(ARGO_API_GROUP) != null
            && APPLICATION_API_VERSION.equals(client.apiextensions().getApiGroup(ARGO_API_GROUP).getApiVersion());
    }
}