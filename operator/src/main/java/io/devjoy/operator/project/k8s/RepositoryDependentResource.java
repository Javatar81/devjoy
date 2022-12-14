package io.devjoy.operator.project.k8s;

import io.devjoy.operator.repository.domain.GitProvider;
import io.devjoy.operator.repository.domain.Visibility;
import io.devjoy.operator.repository.k8s.ManagedSpec;
import io.devjoy.operator.repository.k8s.Repository;
import io.devjoy.operator.repository.k8s.RepositorySpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent
public class RepositoryDependentResource extends CRUDKubernetesDependentResource<Repository, Project>{

	public RepositoryDependentResource() {
		super(Repository.class);
		
	}

	@Override
	protected Repository desired(Project primary, Context<Project> context) {
		Repository repository = new Repository();
		ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
			.withName(primary.getMetadata().getName())
			.withNamespace(primary.getMetadata().getNamespace());
		repository.setMetadata(metaBuilder.build());
		RepositorySpec spec = new RepositorySpec();
		if (StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl())) {
			ManagedSpec managed = new ManagedSpec();
			spec.setManaged(managed);
			managed.setDeleteRepoOnFinalize(true);
			managed.setProvider(GitProvider.GITEA);
			managed.setUser("user-1");
			managed.setVisibility(Visibility.PUBLIC);
		} else {
			spec.setExistingRepositoryCloneUrl(primary.getSpec().getExistingRepositoryCloneUrl());
		}
		repository.setSpec(spec);
		return repository;
	}

}
