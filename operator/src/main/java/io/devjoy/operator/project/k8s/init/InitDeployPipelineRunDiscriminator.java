package io.devjoy.operator.project.k8s.init;

import java.util.Optional;

import io.devjoy.operator.project.k8s.Project;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class InitDeployPipelineRunDiscriminator implements ResourceDiscriminator<PipelineRun, Project>{
    
	TektonClient tektonClient = new DefaultTektonClient();
    
    @Override
    public Optional<PipelineRun> distinguish(Class<PipelineRun> resource, Project primary, Context<Project> context) {
        return Optional.ofNullable(tektonClient.v1beta1()
            .pipelineRuns().inNamespace(primary.getOwningEnvironment(context.getClient()).map(env -> env.getMetadata().getNamespace()).orElseGet(() -> primary.getMetadata().getNamespace())).withName(InitDeployPipelineRunDependentResource.getName(primary)).get());
    }
}