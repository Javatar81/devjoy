package io.devjoy.operator.environment.k8s.build;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.triggers.v1alpha1.TriggerTemplate;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class TriggerTemplateActivationCondition implements Condition<TriggerTemplate, DevEnvironment> {

    @Override
    public boolean isMet(DependentResource<TriggerTemplate, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
       return serverSupportsApi(context.getClient());
    }

    public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(TriggerTemplate.class);
    }
}