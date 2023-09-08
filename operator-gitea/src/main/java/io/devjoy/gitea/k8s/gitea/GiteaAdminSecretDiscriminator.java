package io.devjoy.gitea.k8s.gitea;

import java.util.Optional;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class GiteaAdminSecretDiscriminator implements ResourceDiscriminator<Secret, Gitea>{

    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, Gitea primary, Context<Gitea> context) {
        String adminUser = primary.getSpec().getAdminUser();
        return Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(adminUser + "-git-secret").get());
    }
}
