package io.devjoy.operator.project.k8s;

import java.util.Optional;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;


public class SourceRepositoryDiscriminator implements ResourceDiscriminator<GiteaRepository, Project>{
   
    public Optional<GiteaRepository> distinguish(Class<GiteaRepository> resource, Project primary, Context<Project> context) {
        return Optional.ofNullable(context.getClient().resources(GiteaRepository.class).inNamespace(primary.getMetadata().getNamespace()).withName(SourceRepositoryDependentResource.getName(primary)).get());
    }
}
