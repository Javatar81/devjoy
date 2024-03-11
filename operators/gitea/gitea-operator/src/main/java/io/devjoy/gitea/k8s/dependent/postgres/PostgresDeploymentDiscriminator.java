package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class PostgresDeploymentDiscriminator extends ResourceIDMatcherDiscriminator<Deployment, Gitea> {
	
	public PostgresDeploymentDiscriminator() {
		super(p -> new ResourceID(PostgresDeploymentDependent.getName(p), p.getMetadata().getNamespace()));
	}

}
