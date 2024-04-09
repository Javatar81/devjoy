package io.devjoy.operator.project.k8s.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.operator.project.k8s.Project;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.runtime.util.StringUtil;

public class GitopsRepositoryReadyPostcondition implements Condition<GiteaRepository, Project> {
	private static final Logger LOG = LoggerFactory.getLogger(GitopsRepositoryReadyPostcondition.class);
	
	@Override
	public boolean isMet(DependentResource<GiteaRepository, Project> dependentResource, Project primary,
			Context<Project> context) {
		boolean existingRepoCloneUrl = !StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl());

		boolean giteaRepoCloneUrl = dependentResource.getSecondaryResource(primary, context)
				.map(GiteaRepository::getStatus).map(status -> !StringUtil.isNullOrEmpty(status.getCloneUrl()))
				.orElse(false);
		LOG.debug("Repo urls available, existing={}, gitea={}", existingRepoCloneUrl, giteaRepoCloneUrl);
		return existingRepoCloneUrl || giteaRepoCloneUrl;

	}

}
