package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Optional;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class GiteaDeploymentDiscriminator implements ResourceDiscriminator<Deployment, Gitea>{
 
    @Override
    public Optional<Deployment> distinguish(Class<Deployment> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().apps().deployments().inNamespace(primary.getMetadata().getNamespace()).withName(primary.getMetadata().getName()).get());
    }


    
}
