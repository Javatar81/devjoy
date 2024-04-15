package io.devjoy.gitea.organization.k8s.dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

public class GiteaAdminSecretReadonlyDependent extends KubernetesDependentResource<Secret, GiteaOrganization>{
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretReadonlyDependent.class);
	public GiteaAdminSecretReadonlyDependent() {
		super(Secret.class);
	}

	@Override
	protected ReconcileResult<Secret> reconcile(GiteaOrganization primary, Secret actualResource,
			Context<GiteaOrganization> context) {
		LOG.info("Reconcile {}", actualResource.getMetadata().getName());
		return super.reconcile(primary, actualResource, context);
	}

}

