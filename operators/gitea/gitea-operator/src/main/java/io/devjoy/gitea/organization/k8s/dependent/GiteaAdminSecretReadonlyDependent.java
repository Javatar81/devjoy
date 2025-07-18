package io.devjoy.gitea.organization.k8s.dependent;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

public class GiteaAdminSecretReadonlyDependent extends KubernetesDependentResource<Secret, GiteaOrganization>{
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretReadonlyDependent.class);


	public GiteaAdminSecretReadonlyDependent() {
		super(Secret.class);
	}

	@Override
	protected Secret desired(
		GiteaOrganization primary,
		Context<GiteaOrganization> context) {
			LOG.error("desired");

			return primary.associatedGitea(context.getClient())
				.map(g -> new SecretBuilder()
					.withMetadata(
						new ObjectMetaBuilder()
							.withName(GiteaAdminSecretDependent.getName(g))
							.withNamespace(primary.getMetadata().getNamespace())
							.build())
					.build())
				.orElseThrow(() -> new IllegalStateException("Gitea must be available before secret can be associated"));
	}
}

