package io.devjoy.gitea.k8s.gitea;

import java.util.Optional;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class GiteaServiceDiscriminator implements ResourceDiscriminator<Service, Gitea>{

    @Override
    public Optional<Service> distinguish(Class<Service> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().services().inNamespace(primary.getMetadata().getNamespace()).withName("giteaService").get());
    }


    
}
