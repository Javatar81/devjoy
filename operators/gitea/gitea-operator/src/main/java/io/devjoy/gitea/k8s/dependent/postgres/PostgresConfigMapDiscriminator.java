package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class PostgresConfigMapDiscriminator extends ResourceIDMatcherDiscriminator<ConfigMap, Gitea> {
	
	public PostgresConfigMapDiscriminator() {
		super(p -> new ResourceID(PostgresConfigMapDependent.getName(p), p.getMetadata().getNamespace()));
	}

}
