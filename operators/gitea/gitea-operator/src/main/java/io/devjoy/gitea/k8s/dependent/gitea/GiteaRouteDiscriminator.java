package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaRouteDiscriminator extends ResourceIDMatcherDiscriminator<Route, Gitea> {
    
    public GiteaRouteDiscriminator() {
		super(p -> new ResourceID(GiteaRouteDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
