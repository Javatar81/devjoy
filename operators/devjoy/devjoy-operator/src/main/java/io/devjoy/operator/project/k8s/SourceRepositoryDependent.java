package io.devjoy.operator.project.k8s;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import org.openapi.quarkus.gitea_json.model.CreateHookOption.TypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryLabels;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositorySpec;
import io.devjoy.gitea.repository.k8s.model.SecretReferenceSpec;
import io.devjoy.gitea.repository.k8s.model.WebhookSpec;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependent;
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependent;
import io.fabric8.knative.internal.pkg.apis.duck.v1beta1.Addressable;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.EventListenerStatus;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = SourceRepositoryDiscriminator.class)
public class SourceRepositoryDependent extends CRUDKubernetesDependentResource<GiteaRepository, Project>{
	private static final Logger LOG = LoggerFactory.getLogger(SourceRepositoryDependent.class);
	private static final String ENVIRONMENT_NAME_LABEL_KEY = "devjoy.io/environment.name";
	private static final String ENVIRONMENT_NAMESPACE_LABEL_KEY = "devjoy.io/environment.namespace";

	public SourceRepositoryDependent() {
		super(GiteaRepository.class);
		
	}

	@Override
	protected GiteaRepository desired(Project primary, Context<Project> context) {
		LOG.info("Reconciling source repo for project {} ", primary.getMetadata().getName());
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
			if (e.getSpec() != null && e.getSpec().getGitea() != null) {
				labels.put(GiteaRepositoryLabels.LABEL_GITEA_NAME, e.getSpec().getGitea().getResourceName());
			}
			labels.put(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE, e.getMetadata().getNamespace());
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
							.withName(WebhookSecretDependent.getName(e))
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
		return Optional.ofNullable(BuildEventListenerDependent.getResource(env, client).get())
				.map(EventListener::getStatus)
				.map(EventListenerStatus::getAddress)
				.map(Addressable::getUrl).orElse("");
	}

}
