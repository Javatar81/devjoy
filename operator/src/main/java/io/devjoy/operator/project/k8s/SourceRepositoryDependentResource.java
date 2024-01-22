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
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependentResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = SourceRepositoryDiscriminator.class)
public class SourceRepositoryDependentResource extends CRUDKubernetesDependentResource<GiteaRepository, Project>{

	private static final String ENVIRONMENT_NAME_LABEL_KEY = "devjoy.io/environment.name";
	private static final String ENVIRONMENT_NAMESPACE_LABEL_KEY = "devjoy.io/environment.namespace";

	public SourceRepositoryDependentResource() {
		super(GiteaRepository.class);
		
	}

	@Override
	protected GiteaRepository desired(Project primary, Context<Project> context) {
		GiteaRepository repository = new GiteaRepository();
		ObjectMetaBuilder metaBuilder = new ObjectMetaBuilder()
			.withName(getName(primary))
			.withNamespace(primary.getMetadata().getNamespace());
			Optional<DevEnvironment> env = primary.getOwningEnvironment(context.getClient());
		repository.setMetadata(metaBuilder.build());
		HashMap<String, String> labels = new HashMap<>();
		labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, primary.getSpec().getEnvironmentNamespace());
		labels.put(ENVIRONMENT_NAME_LABEL_KEY, primary.getSpec().getEnvironmentName());
		env.ifPresent(e -> {
			if (StringUtil.isNullOrEmpty(primary.getSpec().getEnvironmentNamespace())) {
				labels.put(ENVIRONMENT_NAMESPACE_LABEL_KEY, e.getMetadata().getNamespace());
			}
			if (StringUtil.isNullOrEmpty(primary.getSpec().getEnvironmentName())) {
				labels.put(ENVIRONMENT_NAME_LABEL_KEY, e.getMetadata().getName());
			}
			if (e.getSpec().getGitea() != null) {
				labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAME, e.getSpec().getGitea().getResourceName());
			}
			labels.put(GiteaRepositoryReconciler.LABEL_GITEA_NAMESPACE, e.getMetadata().getNamespace());
		});
		repository.getMetadata().setLabels(labels);
		
		GiteaRepositorySpec spec = new GiteaRepositorySpec();
		if (StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl())) {
			spec.setDeleteOnFinalize(true);
			if (primary.getSpec().getOwner() != null) {
				spec.setUser(primary.getSpec().getOwner().getUser());
			}
			spec.setVisibility(Visibility.PUBLIC);
			env.filter(e ->  EventListenerActivationCondition.serverSupportsApi(context.getClient()))
				.ifPresent(e -> {
				spec.setWebhooks(List.of(WebhookSpec.builder()
					.withActive(true)
					.withEvents(List.of("push"))
					.withTargetUrl(getEventListenerUrl(e, context.getClient()))
					.withBranchFilter("*")
					.withHttpMethod("POST")
					.withType(TypeEnum.GITEA.toString().toUpperCase())
					.withSecretRef(SecretReferenceSpec.builder()
							.withKey("webhook-secret")
							.withName(WebhookSecretDependentResource.getName(e))
							.withNamespace(e.getMetadata().getNamespace())
							.build()
					).build()
			));
			});
			
		} else {
			//TODOspec.setExistingRepositoryCloneUrl(primary.getSpec().getExistingRepositoryCloneUrl());
		}
		repository.setSpec(spec);
		return repository;
	}

	public static String getName(Project primary) {
		return primary.getMetadata().getName();
	}
	
	private String getEventListenerUrl(DevEnvironment env, KubernetesClient client) {
		return BuildEventListenerDependentResource.getResource(env, client)
				.waitUntilCondition(el -> el != null && el.getStatus() != null && !StringUtil.isNullOrEmpty(el.getStatus().getAddress().getUrl()),
						10, TimeUnit.SECONDS)
				.getStatus().getAddress().getUrl();
	}

}
