package io.devjoy.gitea.organization.k8s.dependent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.runtime.util.StringUtil;

public class GiteaAdminSecretReadyPostcondition implements Condition<Secret, GiteaOrganization> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretReadyPostcondition.class);

	@Override
	public boolean isMet(DependentResource<Secret, GiteaOrganization> dependentResource, GiteaOrganization primary,
			Context<GiteaOrganization> context) {
		boolean present = dependentResource.getSecondaryResource(primary, context)
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.filter(t -> !StringUtil.isNullOrEmpty(t))
				.isPresent();
		LOG.debug("Postcondition is {}", present);
		return present;
	}

}
