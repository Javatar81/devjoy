package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class GiteaDependentResource extends CRUDKubernetesDependentResource<Gitea, DevEnvironment> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaDependentResource.class);

	public GiteaDependentResource() {
		super(Gitea.class);
	}

	@Override
	public Gitea create(Gitea target, DevEnvironment primary, Context<DevEnvironment> context) {
		if(enabledAndManaged(primary)) {
			return super.create(target, primary, context);
		} else {
			return getResource(context.getClient(), primary).get();
		}
	}
	
	@Override
	protected Gitea desired(DevEnvironment primary, Context<DevEnvironment> context) {
		if (enabledAndManaged(primary)) {
			Gitea gitea = context.getClient().resources(Gitea.class)
					.load(getClass().getClassLoader().getResourceAsStream("dev/gitea.yaml")).item();
			gitea.getMetadata().setNamespace(primary.getMetadata().getNamespace());
			gitea.getMetadata().setName(generateGiteaName(primary));
			return gitea;
		} else {
			LOG.info("Gitea is not managed for {}", primary.getMetadata().getName());
			Gitea gitea = getResource(context.getClient(), primary).get();
			if (gitea == null && LOG.isWarnEnabled()) {
				LOG.warn("Gitea not found. Set gitea to managed state or install it in namespace {} with name {}.",
						primary.getMetadata().getNamespace(), generateGiteaName(primary));
			}
			return gitea;
		}
	}

	private static String generateGiteaName(DevEnvironment primary) {
		if (primary.getSpec().getGitea() != null && primary.getSpec().getGitea().getResourceName() != null) {
			return primary.getSpec().getGitea().getResourceName();
		} else {
			return primary.getMetadata().getNamespace() + "-" + primary.getMetadata().getName();
		}
	}

	private boolean enabledAndManaged(DevEnvironment primary) {
		return primary.getSpec() == null || primary.getSpec().getGitea() == null
				|| (primary.getSpec().getGitea().isEnabled() && primary.getSpec().getGitea().isManaged());
	}
	
	public static Resource<Gitea> getResource(KubernetesClient client, DevEnvironment env) {
		return client.resources(Gitea.class).inNamespace(env.getMetadata().getNamespace())
				.withName(generateGiteaName(env));
	}
}
