package io.devjoy.operator.project.k8s;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;


public class SourceRepositoryDiscriminator extends ResourceIDMatcherDiscriminator<GiteaRepository, Project> {
    
	public SourceRepositoryDiscriminator() {
		super(p -> new ResourceID(SourceRepositoryDependent.getName(p), p.getMetadata().getNamespace()));
	}
   
}

