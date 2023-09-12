package io.devjoy.gitea.k8s.postgres;

import java.util.Optional;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class PostgresPvcDiscriminator implements ResourceDiscriminator<PersistentVolumeClaim, Gitea>{

    @Override
    public Optional<PersistentVolumeClaim> distinguish(Class<PersistentVolumeClaim> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().persistentVolumeClaims().inNamespace(primary.getMetadata().getNamespace()).withName(PostgresPvcDependentResource.getName(primary)).get());
    }


    
}
