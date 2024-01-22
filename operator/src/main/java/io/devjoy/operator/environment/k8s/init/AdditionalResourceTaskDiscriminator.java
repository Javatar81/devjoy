package io.devjoy.operator.environment.k8s.init;

import java.util.Optional;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.DefaultTektonClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.Task;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class AdditionalResourceTaskDiscriminator implements ResourceDiscriminator<Task, DevEnvironment>{
    
    TektonClient tektonClient = new DefaultTektonClient();

    @Override
    public Optional<Task> distinguish(Class<Task> resource, DevEnvironment primary, Context<DevEnvironment> context) {
        return Optional.ofNullable(tektonClient.v1()
            .tasks().inNamespace(primary.getMetadata().getNamespace()).withName("additional-resources").get());
    }
}
