package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaAdminSecretDiscriminator extends ResourceIDMatcherDiscriminator<Secret, Gitea> {

	public GiteaAdminSecretDiscriminator() {
		super(p -> new ResourceID(GiteaAdminSecretDependent.getName(p), p.getMetadata().getNamespace()));
	}
}
