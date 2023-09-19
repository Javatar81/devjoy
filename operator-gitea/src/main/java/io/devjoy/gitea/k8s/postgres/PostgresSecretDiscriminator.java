package io.devjoy.gitea.k8s.postgres;

import java.util.Optional;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class PostgresSecretDiscriminator implements ResourceDiscriminator<Secret, Gitea>{

    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(PostgresSecretDependentResource.getName(primary)).get());
    }


    
}
