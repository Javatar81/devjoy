package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(resourceDiscriminator = GiteaDeploymentDiscriminator.class,labelSelector = GiteaServiceDependent.LABEL_SELECTOR)
public class GiteaServiceDependent extends CRUDKubernetesDependentResource<Service, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaServiceDependent.class);
	private static final String LABEL_KEY = "devjoy.io/svc.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public GiteaServiceDependent() {
		super(Service.class);
	}

	@Override
	protected Service desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea service");
		Service svc = context.getClient().services()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/service.yaml")).item();
		String name = getName(primary);
		svc.getMetadata().setName(name);
		LOG.info("Name is {} ", name);
		svc.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		svc.getSpec().getSelector().put("name", name);
		if (svc.getMetadata().getLabels() == null) {
			svc.getMetadata().setLabels(new HashMap<>());
		}
		svc.getMetadata().getLabels().put("app", name);
		svc.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return svc;
	}
	
	public static String getName(Gitea primary) {
		return primary.getMetadata().getName();
	}

	public static Resource<Service> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(Service.class).inNamespace(primary.getMetadata().getNamespace()).withName(
			getName(primary));
	}

}
