package io.devjoy.operator.project.k8s.init;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.operator.project.k8s.Project;
import io.devjoy.operator.project.k8s.SourceRepositoryDiscriminator;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.runtime.util.StringUtil;

public class SourceRepositoryReadyPostcondition implements Condition<GiteaRepository, Project> {
	private SourceRepositoryDiscriminator sourceRepoDiscriminator = new SourceRepositoryDiscriminator();
	private static final Logger LOG = LoggerFactory.getLogger(SourceRepositoryReadyPostcondition.class);
	@Override
	public boolean isMet(DependentResource<GiteaRepository, Project> dependentResource, Project primary,
			Context<Project> context) {
		boolean existingRepoCloneUrl = !StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl());
				
		Optional<GiteaRepository> sourceRepo = dependentResource.getSecondaryResource(primary, context);
		
		boolean giteaRepoCloneUrl = dependentResource.getSecondaryResource(primary, context).map(GiteaRepository::getStatus)
			.map(status -> !StringUtil.isNullOrEmpty(status.getCloneUrl())).orElse(false);
		
		sourceRepo.ifPresentOrElse(r -> LOG.info("Urls available for repo '{}', existing={}, gitea={}", r.getMetadata().getName(), existingRepoCloneUrl, giteaRepoCloneUrl), () -> LOG.info("Repo not available"));
		
		return existingRepoCloneUrl || giteaRepoCloneUrl;
	}

}
