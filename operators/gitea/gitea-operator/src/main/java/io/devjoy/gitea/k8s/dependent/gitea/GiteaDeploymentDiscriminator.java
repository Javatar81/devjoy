package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaDeploymentDiscriminator extends ResourceIDMatcherDiscriminator<Deployment, Gitea> {
 
    public GiteaDeploymentDiscriminator() {
		super(p -> new ResourceID(GiteaDeploymentDependent.getName(p), p.getMetadata().getNamespace()));
	}

}
