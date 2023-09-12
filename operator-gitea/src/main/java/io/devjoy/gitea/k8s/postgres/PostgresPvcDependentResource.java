package io.devjoy.gitea.k8s.postgres;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = PostgresPvcDiscriminator.class, labelSelector = PostgresPvcDependentResource.LABEL_SELECTOR)
public class PostgresPvcDependentResource extends CRUDKubernetesDependentResource<PersistentVolumeClaim, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(PostgresPvcDependentResource.class);
	private static final String LABEL_KEY = "devjoy.io/pvc.target";
	private static final String LABEL_VALUE = "postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public PostgresPvcDependentResource() {
		super(PersistentVolumeClaim.class);
		
	}

	public static String getName(Gitea primary) {
		return "postgresql-" + primary.getMetadata().getName().toLowerCase() + "-pvc";
	}

	@Override
	protected PersistentVolumeClaim desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Postgres pvc");
		PersistentVolumeClaim pvc = client.persistentVolumeClaims()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/postgres/pvc.yaml"))
				.item();
		String name = getName(primary);
		pvc.getMetadata().setName(name);
		pvc.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (primary.getSpec() != null) {
			if (primary.getSpec().getPostgres() != null && !StringUtil.isNullOrEmpty(primary.getSpec().getPostgres().getVolumeSize())) {
				pvc.getSpec().getResources().getRequests().put("storage", Quantity.parse(primary.getSpec().getPostgres().getVolumeSize()));
			}
			if (primary.getSpec().getPostgres() != null && !StringUtil.isNullOrEmpty(primary.getSpec().getPostgres().getStorageClass())) {
				pvc.getSpec().setStorageClassName(primary.getSpec().getPostgres().getStorageClass());
			}
		}
		if (pvc.getMetadata().getLabels() == null) {
			pvc.getMetadata().setLabels(new HashMap<>());
		}
		pvc.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return pvc;
	}
}
