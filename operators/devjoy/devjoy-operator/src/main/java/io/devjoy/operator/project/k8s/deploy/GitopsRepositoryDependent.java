package io.devjoy.operator.project.k8s.deploy;

import java.util.HashMap;
import java.util.Optional;

import io.devjoy.gitea.k8s.domain.GiteaLabels;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositorySpec;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependent;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(informer = @Informer(labelSelector = GitopsRepositoryDependent.LABEL_SELECTOR))
public class GitopsRepositoryDependent extends CRUDKubernetesDependentResource<GiteaRepository, Project>{

	private static final String ENVIRONMENT_NAME_LABEL_KEY = "devjoy.io/environment.name";
	private static final String ENVIRONMENT_NAMESPACE_LABEL_KEY = "devjoy.io/environment.namespace";
    public static final String REPO_POSTFIX = "-app";
	private static final String LABEL_KEY = "devjoy.io/repository.type";
	private static final String LABEL_VALUE = "gitops";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;

	public GitopsRepositoryDependent() {
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
		Optional<DevEnvironment> env = primary.getOwningEnvironment(context.getClient());
		labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, primary.getSpec().getEnvironmentNamespace());
		labels.put(ENVIRONMENT_NAME_LABEL_KEY, primary.getSpec().getEnvironmentName());
		labels.put(LABEL_KEY, LABEL_VALUE);
		env.ifPresent(e -> {
			if (StringUtil.isNullOrEmpty(primary.getSpec().getEnvironmentNamespace())) {
				labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, e.getMetadata().getNamespace());
			}
			if (StringUtil.isNullOrEmpty(primary.getSpec().getEnvironmentName())) {
				labels.put(ENVIRONMENT_NAME_LABEL_KEY, e.getMetadata().getName());
			}
			if (e.getSpec() != null && e.getSpec().getGitea() != null) {
				labels.put(GiteaLabels.LABEL_GITEA_NAME, e.getSpec().getGitea().getResourceName());
			}
			labels.put(GiteaLabels.LABEL_GITEA_NAMESPACE, e.getMetadata().getNamespace());
		});
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
	
}