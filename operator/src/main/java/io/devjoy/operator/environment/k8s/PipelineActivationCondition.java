package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PipelineActivationCondition implements Condition<Pipeline, DevEnvironment> {
   private static final Logger LOG = LoggerFactory.getLogger(PipelineActivationCondition.class);
   
   public static final String PIPELINES_API_VERSION = "v1";
    public static final String PIPELINES_API_GROUP = "tekton.dev";
    @Override
    public boolean isMet(DependentResource<Pipeline, DevEnvironment> dependentResource, DevEnvironment primary, Context<DevEnvironment> context) {
      return isPipelinesApiAvailable(context.getClient());
    }

    public static boolean isPipelinesApiAvailable(KubernetesClient client) {
		return client.apiextensions().getApiGroup(PIPELINES_API_GROUP) != null
            && PIPELINES_API_VERSION.equals(client.apiextensions().getApiGroup(PIPELINES_API_GROUP).getApiVersion());
	}
}