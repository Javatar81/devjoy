package io.devjoy.operator.project.k8s;

import java.util.Optional;

import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class InitPipelineRunDiscriminator implements ResourceDiscriminator<PipelineRun, Project>{
    
	TektonClient tektonClient = new DefaultTektonClient();
    
    @Override
    public Optional<PipelineRun> distinguish(Class<PipelineRun> resource, Project primary, Context<Project> context) {
        return Optional.ofNullable(tektonClient.v1beta1()
            .pipelineRuns().inNamespace(primary.getMetadata().getNamespace()).withName(InitPipelineRunDependentResource.getName(primary)).get());
    }
}