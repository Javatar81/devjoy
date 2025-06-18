package io.devjoy.operator.environment.k8s;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.v1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PipelineActivationCondition implements Condition<Pipeline, DevEnvironment> {
   
  @Override
  public boolean isMet(DependentResource<Pipeline, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
    return serverSupportsApi(context.getClient());
  }

  public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(Pipeline.class);
  }
}