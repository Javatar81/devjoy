package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaPvcDiscriminator extends ResourceIDMatcherDiscriminator<PersistentVolumeClaim, Gitea> {

    public GiteaPvcDiscriminator() {
		super(p -> new ResourceID(GiteaPvcDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
