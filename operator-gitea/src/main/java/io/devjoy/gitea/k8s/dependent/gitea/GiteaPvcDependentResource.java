package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaim;
import io.fabric8.kubernetes.api.model.Quantity;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = GiteaPvcDiscriminator.class, labelSelector = GiteaPvcDependentResource.LABEL_SELECTOR)
public class GiteaPvcDependentResource extends CRUDKubernetesDependentResource<PersistentVolumeClaim, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaPvcDependentResource.class);
	private static final String LABEL_KEY = "devjoy.io/pvc.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public GiteaPvcDependentResource() {
		super(PersistentVolumeClaim.class);
		
	}

	public static String getName(Gitea primary) {
		return primary.getMetadata().getName() + "-pvc";
	}

	@Override
	protected PersistentVolumeClaim desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea pvc");
		PersistentVolumeClaim pvc = context.getClient().persistentVolumeClaims()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/pvc.yaml"))
				.item();
		String name = getName(primary);
		pvc.getMetadata().setName(name);
		pvc.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (primary.getSpec() != null) {
			if (primary.getSpec() != null && !StringUtil.isNullOrEmpty(primary.getSpec().getVolumeSize())) {
				pvc.getSpec().getResources().getRequests().put("storage", Quantity.parse(primary.getSpec().getVolumeSize()));
			}
			if (primary.getSpec() != null && !StringUtil.isNullOrEmpty(primary.getSpec().getStorageClass())) {
				pvc.getSpec().setStorageClassName(primary.getSpec().getStorageClass());
			}
		}
		if (pvc.getMetadata().getLabels() == null) {
			pvc.getMetadata().setLabels(new HashMap<>());
		}
		pvc.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return pvc;
	}
}

