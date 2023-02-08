package io.devjoy.gitea.k8s.postgres;

import java.util.HashMap;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = PostgresServiceDependentResource.LABEL_SELECTOR)
public class PostgresServiceDependentResource extends CRUDKubernetesDependentResource<Service, Gitea>{
	private static final String LABEL_KEY = "devjoy.io/svc.target";
	private static final String LABEL_VALUE = "postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public PostgresServiceDependentResource() {
		super(Service.class);
	}
	
	@Override
	protected Service desired(Gitea primary, Context<Gitea> context) {
		Service svc = client.services()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/postgres/service.yaml"))
				.get();
		String name = svc.getMetadata().getName() + primary.getMetadata().getName();
		svc.getMetadata().setName(name);
		svc.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		svc.getSpec().getSelector().put("name", name);
		if (svc.getMetadata().getLabels() == null) {
			svc.getMetadata().setLabels(new HashMap<>());
		}
		svc.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return svc;
	}

}
