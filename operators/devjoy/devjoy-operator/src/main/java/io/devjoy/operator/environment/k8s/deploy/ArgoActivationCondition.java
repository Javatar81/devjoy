package io.devjoy.operator.environment.k8s.deploy;

import io.argoproj.v1beta1.ArgoCD;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ArgoActivationCondition implements Condition<ArgoCD, DevEnvironment> {

    @Override
    public boolean isMet(DependentResource<ArgoCD, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
       return serverSupportsApi(context.getClient());
    }

    public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(ArgoCD.class);
    }

}