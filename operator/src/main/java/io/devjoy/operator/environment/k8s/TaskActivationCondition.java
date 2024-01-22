package io.devjoy.operator.environment.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.pipeline.v1.Task;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class TaskActivationCondition implements Condition<Task, DevEnvironment> {

    @Override
    public boolean isMet(DependentResource<Task, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
       return serverSupportsApi(context.getClient());
    }

    public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(Task.class);
    }
}