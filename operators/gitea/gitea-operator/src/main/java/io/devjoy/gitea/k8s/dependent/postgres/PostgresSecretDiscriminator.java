package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class PostgresSecretDiscriminator extends ResourceIDMatcherDiscriminator<Secret, Gitea> {
	
	public PostgresSecretDiscriminator() {
		super(p -> new ResourceID(PostgresSecretDependent.getName(p), p.getMetadata().getNamespace()));
	}

}
