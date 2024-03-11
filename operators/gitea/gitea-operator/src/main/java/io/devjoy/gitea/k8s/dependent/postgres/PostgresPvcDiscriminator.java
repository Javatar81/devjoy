package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class PostgresPvcDiscriminator  extends ResourceIDMatcherDiscriminator<PersistentVolumeClaim, Gitea> {

	public PostgresPvcDiscriminator() {
		super(p -> new ResourceID(PostgresPvcDependent.getName(p), p.getMetadata().getNamespace()));
	}
	
}
