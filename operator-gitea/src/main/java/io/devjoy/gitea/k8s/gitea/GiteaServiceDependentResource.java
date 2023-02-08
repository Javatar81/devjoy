package io.devjoy.gitea.k8s.gitea;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = GiteaServiceDependentResource.LABEL_SELECTOR)
public class GiteaServiceDependentResource extends CRUDKubernetesDependentResource<Service, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaServiceDependentResource.class);
	private static final String LABEL_KEY = "devjoy.io/svc.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public GiteaServiceDependentResource() {
		super(Service.class);
	}

	@Override
	protected Service desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea service");
		Service svc = client.services()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/service.yaml")).get();
		String name = primary.getMetadata().getName();
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

}
