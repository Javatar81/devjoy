package io.devjoy.gitea.k8s.dependent.postgres;

import java.util.HashMap;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Service;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(informer = @Informer(labelSelector = PostgresServiceDependent.LABEL_SELECTOR))
public class PostgresServiceDependent extends CRUDKubernetesDependentResource<Service, Gitea>{
	private static final String LABEL_KEY = "devjoy.io/svc.target";
	private static final String LABEL_VALUE = "postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	private static final String OCP_SERVICE_CERTIFICATE_LABEL = "service.beta.openshift.io/serving-cert-secret-name";
	
	
	public PostgresServiceDependent() {
		super(Service.class);
	}
	
	@Override
	protected Service desired(Gitea primary, Context<Gitea> context) {
		Service svc = context.getClient().services()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/postgres/service.yaml"))
				.item();
		String name = getName(primary);
		svc.getMetadata().setName(name);
		svc.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		svc.getSpec().getSelector().put("name", name);
		if (svc.getMetadata().getLabels() == null) {
			svc.getMetadata().setLabels(new HashMap<>());
		}
		svc.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		if (primary.getSpec() != null && primary.getSpec().getPostgres().isSsl()) {
			svc.getMetadata().getAnnotations().put(OCP_SERVICE_CERTIFICATE_LABEL, getServiceCertSecretName(primary));
		}
		return svc;
	}

	public static String getName(Gitea primary) {
		return "postgresql-" + primary.getMetadata().getName();
	}

	public static String getServiceCertSecretName(Gitea primary) {
		return getName(primary) + "-cert";
	}


	
}
