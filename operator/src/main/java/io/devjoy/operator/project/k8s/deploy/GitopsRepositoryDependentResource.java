package io.devjoy.operator.project.k8s.deploy;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaRepositoryReconciler;
import io.devjoy.gitea.repository.k8s.GiteaRepositorySpec;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = GitopsRepositoryDiscriminator.class)
public class GitopsRepositoryDependentResource extends CRUDKubernetesDependentResource<GiteaRepository, Project>{

	private static final String ENVIRONMENT_NAME_LABEL_KEY = "devjoy.io/environment.name";
	private static final String ENVIRONMENT_NAMESPACE_LABEL_KEY = "devjoy.io/environment.namespace";
    public static final String REPO_POSTFIX = "-app";

	public GitopsRepositoryDependentResource() {
		super(GiteaRepository.class);
		
	}

	@Override
	protected GiteaRepository desired(Project primary, Context<Project> context) {
		GiteaRepository repository = new GiteaRepository();
		ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
			.withName(getName(primary))
			.withNamespace(primary.getMetadata().getNamespace());
		repository.setMetadata(metaBuilder.build());
		HashMap<String, String> labels = new HashMap<>();
		if (primary.getSpec().getEnvironmentNamespace() != null) {
			DevEnvironment env = getOwningEnvironment(primary).waitUntilCondition(c -> c != null, 120, TimeUnit.SECONDS);
			labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAME, env.getSpec().getGitea().getResourceName());
			labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAMESPACE, env.getMetadata().getNamespace());
		}
		labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, primary.getSpec().getEnvironmentNamespace());
		labels.put(ENVIRONMENT_NAME_LABEL_KEY, primary.getSpec().getEnvironmentName());
		repository.getMetadata().setLabels(labels);
		
		GiteaRepositorySpec spec = new GiteaRepositorySpec();
		if (StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl())) {
			spec.setDeleteOnFinalize(true);
			if (primary.getSpec().getOwner() != null) {
				spec.setUser(primary.getSpec().getOwner().getUser());
			}
			spec.setVisibility(Visibility.PUBLIC);
		} else {
			//TODOspec.setExistingRepositoryCloneUrl(primary.getSpec().getExistingRepositoryCloneUrl());
		}
		repository.setSpec(spec);
		return repository;
	}

    public static String getName(Project primary) {
		return primary.getMetadata().getName() + REPO_POSTFIX;
	}
	
	private Resource<DevEnvironment> getOwningEnvironment(Project owningProject) {
		return 
				client.resources(DevEnvironment.class).inNamespace(owningProject.getSpec().getEnvironmentNamespace())
						.withName(owningProject.getSpec().getEnvironmentName());
	}
}