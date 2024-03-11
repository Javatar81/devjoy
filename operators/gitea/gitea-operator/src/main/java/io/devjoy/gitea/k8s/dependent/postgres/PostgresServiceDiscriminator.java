package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class PostgresServiceDiscriminator extends ResourceIDMatcherDiscriminator<Service, Gitea> {
	
	public PostgresServiceDiscriminator() {
		super(p -> new ResourceID(PostgresServiceDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
