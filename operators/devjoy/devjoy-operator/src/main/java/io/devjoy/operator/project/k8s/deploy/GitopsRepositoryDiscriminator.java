package io.devjoy.operator.project.k8s.deploy;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.operator.project.k8s.Project;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;


public class GitopsRepositoryDiscriminator extends ResourceIDMatcherDiscriminator<GiteaRepository, Project> {
    
	public GitopsRepositoryDiscriminator() {
		super(p -> new ResourceID(GitopsRepositoryDependent.getName(p), p.getMetadata().getNamespace()));
	}
   
}

