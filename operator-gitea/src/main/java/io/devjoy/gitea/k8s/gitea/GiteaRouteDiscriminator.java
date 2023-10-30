package io.devjoy.gitea.k8s.gitea;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class GiteaRouteDiscriminator implements ResourceDiscriminator<Route, Gitea>{
    private static final Logger LOG = LoggerFactory.getLogger(GiteaConfigSecretDiscriminator.class);
    @Override
    public Optional<Route> distinguish(Class<Route> resource, Gitea primary, Context<Gitea> context) {
        LOG.debug("Distinguish {} for Gitea primary {} ", resource, primary);
        Optional<Route> result = Optional.ofNullable(context.getClient().resources(Route.class).inNamespace(primary.getMetadata().getNamespace()).withName(GiteaRouteDependentResource.getName(primary)).get());
        LOG.debug("Result is {} ", result);
        return result;
    }
}
