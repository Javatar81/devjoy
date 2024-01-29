package io.devjoy.operator.environment.k8s.init;

import java.util.Optional;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class AdditionalResourcesConfigmapDiscriminator implements ResourceDiscriminator<ConfigMap, DevEnvironment>{
    
    
    @Override
    public Optional<ConfigMap> distinguish(Class<ConfigMap> resource, DevEnvironment primary, Context<DevEnvironment> context) {
        return Optional.ofNullable(context.getClient()
            .configMaps().inNamespace(primary.getMetadata().getNamespace()).withName(AdditionalResourcesConfigmapDependentResource.getName(primary)).get());
    }
}
