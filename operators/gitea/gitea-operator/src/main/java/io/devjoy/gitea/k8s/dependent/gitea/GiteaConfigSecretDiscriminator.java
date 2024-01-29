package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;


public class GiteaConfigSecretDiscriminator implements ResourceDiscriminator<Secret, Gitea>{
    private static final Logger LOG = LoggerFactory.getLogger(GiteaConfigSecretDiscriminator.class);
    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, Gitea primary, Context<Gitea> context) {
        LOG.debug("Distinguish {} for Gitea primary {} ", resource, primary);
        Optional<Secret> result = Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(GiteaConfigSecretDependentResource.getName(primary)).get());
        LOG.debug("Result is {} ", result);
        return result;
    }
}
