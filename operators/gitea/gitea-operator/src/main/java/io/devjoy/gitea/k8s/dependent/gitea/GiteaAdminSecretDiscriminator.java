package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Optional;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class GiteaAdminSecretDiscriminator implements ResourceDiscriminator<Secret, Gitea>{

    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(GiteaAdminSecretDependentResource.getName(primary)).get());
    }
}
