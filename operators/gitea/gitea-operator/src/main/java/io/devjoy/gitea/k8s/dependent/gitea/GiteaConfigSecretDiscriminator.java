package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;


public class GiteaConfigSecretDiscriminator extends ResourceIDMatcherDiscriminator<Secret, Gitea> {
    
	public GiteaConfigSecretDiscriminator() {
		super(p -> new ResourceID(GiteaConfigSecretDependent.getName(p), p.getMetadata().getNamespace()));
	}
	
}
