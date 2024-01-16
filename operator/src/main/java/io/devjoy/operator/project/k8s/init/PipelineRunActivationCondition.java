package io.devjoy.operator.project.k8s.init;

import io.devjoy.operator.environment.k8s.PipelineActivationCondition;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PipelineRunActivationCondition implements Condition<PipelineRun, Project> {
    public static String PIPELINES_API_VERSION = "v1";
    public static String PIPELINES_API_GROUP = "tekton.dev";
    @Override
    public boolean isMet(DependentResource<PipelineRun, Project> dependentResource, Project primary, Context<Project> context) {
       return isPipelinesApiAvailable(context.getClient());
    }

    public static boolean isPipelinesApiAvailable(KubernetesClient client) {
		return client.apiextensions().getApiGroup(PIPELINES_API_GROUP) != null
            && PIPELINES_API_VERSION.equals(client.apiextensions().getApiGroup(PIPELINES_API_GROUP).getApiVersion());
	}
}