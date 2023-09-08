package io.devjoy.gitea.k8s.gitea;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.javaoperatorsdk.operator.processing.event.source.informer.InformerEventSource;

public class GiteaConfigSecretDiscriminator implements ResourceDiscriminator<Secret, Gitea>{
    private static final Logger LOG = LoggerFactory.getLogger(GiteaConfigSecretDiscriminator.class);
    @Override
    public Optional<Secret> distinguish(Class<Secret> resource, Gitea primary, Context<Gitea> context) {
        LOG.debug("Distinguish {} for Gitea primary {} ", resource, primary);
        Optional<Secret> result = Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(primary.getMetadata().getName() + "-config").get());
        LOG.debug("Result is {} ", result);
        return result;
    }
}
