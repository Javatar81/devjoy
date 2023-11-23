package io.devjoy.operator.environment.k8s.deploy;

import java.util.Optional;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class InitDeployPipeplineDiscriminator implements ResourceDiscriminator<Pipeline, DevEnvironment>{
    
	TektonClient tektonClient = new DefaultTektonClient();
    
    @Override
    public Optional<Pipeline> distinguish(Class<Pipeline> resource, DevEnvironment primary, Context<DevEnvironment> context) {
        return Optional.ofNullable(tektonClient.v1beta1()
            .pipelines().inNamespace(primary.getMetadata().getNamespace()).withName(InitDeployPipelineDependentResource.getName(primary)).get());
    }
}
