package io.devjoy.operator.project.k8s;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.openapi.quarkus.gitea_json.model.CreateHookOption.TypeEnum;

import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaRepositoryReconciler;
import io.devjoy.gitea.repository.k8s.GiteaRepositorySpec;
import io.devjoy.gitea.repository.k8s.SecretReferenceSpec;
import io.devjoy.gitea.repository.k8s.WebhookSpec;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependentResource;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependentResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent
public class RepositoryDependentResource extends CRUDKubernetesDependentResource<GiteaRepository, Project>{

	private static final String ENVIRONMENT_NAME_LABEL_KEY = "devjoy.io/environment.name";
	private static final String ENVIRONMENT_NAMESPACE_LABEL_KEY = "devjoy.io/environment.namespace";

	public RepositoryDependentResource() {
		super(GiteaRepository.class);
		
	}

	@Override
	protected GiteaRepository desired(Project primary, Context<Project> context) {
		GiteaRepository repository = new GiteaRepository();
		ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
			.withName(primary.getMetadata().getName())
			.withNamespace(primary.getMetadata().getNamespace());
		repository.setMetadata(metaBuilder.build());
		DevEnvironment env = getOwningEnvironment(primary).waitUntilCondition(c -> c != null, 120, TimeUnit.SECONDS);
		HashMap<String, String> labels = new HashMap<>();
		labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, primary.getSpec().getEnvironmentNamespace());
		labels.put(ENVIRONMENT_NAME_LABEL_KEY, primary.getSpec().getEnvironmentName());
		labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAME, env.getSpec().getGitea().getResourceName());
		labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAMESPACE, env.getMetadata().getNamespace());
		repository.getMetadata().setLabels(labels);
		
		GiteaRepositorySpec spec = new GiteaRepositorySpec();
		if (StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl())) {
			spec.setDeleteOnFinalize(true);
			spec.setUser(primary.getSpec().getOwner().getUser());
			spec.setVisibility(Visibility.PUBLIC);
			
			spec.setWebhooks(List.of(WebhookSpec.builder()
					.withActive(true)
					.withEvents(List.of("push"))
					.withTargetUrl(getEventListenerUrl(env))
					.withBranchFilter("*")
					.withHttpMethod("POST")
					.withType(TypeEnum.GITEA.toString().toUpperCase())
					.withSecretRef(SecretReferenceSpec.builder()
							.withKey("webhook-secret")
							.withName(WebhookSecretDependentResource.getName(env))
							.withNamespace(env.getMetadata().getNamespace())
							.build()
					).build()
			));
		} else {
			//TODOspec.setExistingRepositoryCloneUrl(primary.getSpec().getExistingRepositoryCloneUrl());
		}
		repository.setSpec(spec);
		return repository;
	}
	
	private String getEventListenerUrl(DevEnvironment env) {
		return BuildEventListenerDependentResource.getResource(env, client)
				.waitUntilCondition(el -> el != null && el.getStatus() != null && !StringUtil.isNullOrEmpty(el.getStatus().getAddress().getUrl()),
						10, TimeUnit.SECONDS)
				.getStatus().getAddress().getUrl();
	}
	
	private Resource<DevEnvironment> getOwningEnvironment(Project owningProject) {
		return 
				client.resources(DevEnvironment.class).inNamespace(owningProject.getSpec().getEnvironmentNamespace())
						.withName(owningProject.getSpec().getEnvironmentName());
	}
}
