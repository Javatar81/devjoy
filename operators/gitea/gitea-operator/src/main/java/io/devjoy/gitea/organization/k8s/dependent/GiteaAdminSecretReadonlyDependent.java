package io.devjoy.gitea.organization.k8s.dependent;

import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

public class GiteaAdminSecretReadonlyDependent extends KubernetesDependentResource<Secret, GiteaOrganization>{

	public GiteaAdminSecretReadonlyDependent() {
		super(Secret.class);
	}
}

