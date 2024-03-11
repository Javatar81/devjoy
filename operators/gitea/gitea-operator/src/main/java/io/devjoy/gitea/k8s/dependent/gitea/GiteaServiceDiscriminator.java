package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaServiceDiscriminator extends ResourceIDMatcherDiscriminator<Route, Gitea> {
    
    public GiteaServiceDiscriminator() {
		super(p -> new ResourceID(GiteaServiceDependent.getName(p), p.getMetadata().getNamespace()));
	}

}
