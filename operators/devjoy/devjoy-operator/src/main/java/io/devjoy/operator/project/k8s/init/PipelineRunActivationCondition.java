package io.devjoy.operator.project.k8s.init;

import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.pipeline.v1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PipelineRunActivationCondition implements Condition<PipelineRun, Project> {

    @Override
    public boolean isMet(DependentResource<PipelineRun, Project> dependentResource, Project primary, Context<Project> context) {
        return serverSupportsApi(context.getClient()) && primary.getOwningEnvironment(context.getClient()).isPresent();
    }

    public static boolean serverSupportsApi(KubernetesClient client) {
        return client.supports(PipelineRun.class);
    }

}