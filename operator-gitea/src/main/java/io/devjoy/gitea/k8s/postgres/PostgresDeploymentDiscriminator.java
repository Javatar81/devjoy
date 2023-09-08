package io.devjoy.gitea.k8s.postgres;

import java.util.Optional;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;

public class PostgresDeploymentDiscriminator implements ResourceDiscriminator<Deployment, Gitea>{
 
    @Override
    public Optional<Deployment> distinguish(Class<Deployment> resource, Gitea primary, Context<Gitea> context) {
        return Optional.ofNullable(context.getClient().apps().deployments().inNamespace(primary.getMetadata().getNamespace()).withName("postgresDeployment").get());
    }


    
}
