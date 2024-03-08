package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

@KubernetesDependent(resourceDiscriminator = GiteaTrustMapDiscriminator.class, labelSelector = GiteaServiceDependentResource.LABEL_SELECTOR)
public class GiteaTrustMapDependentResource extends KubernetesDependentResource<ConfigMap, Gitea> implements Creator<ConfigMap, Gitea>, GarbageCollected<Gitea>{
	private static final String GITEA_TRUST_BUNDLE_MAP_NAME = "-trust-bundle";
	private static final String LABEL_KEY = "devjoy.io/cm.role";
	private static final String LABEL_VALUE = "trustmap";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public GiteaTrustMapDependentResource() {
		super(ConfigMap.class);
	}
	
	@Override
	protected ConfigMap desired(Gitea primary, Context<Gitea> context) {
		return new ConfigMapBuilder()
			.withNewMetadata()
			.withName(getName(primary))
			.withNamespace(primary.getMetadata().getNamespace())
			.addToLabels("config.openshift.io/inject-trusted-cabundle", "true")
			.addToLabels(LABEL_KEY, LABEL_VALUE)
			.endMetadata().build();
	}
	
	public static String getName(Gitea primary) {
		return primary.getMetadata().getName() + GITEA_TRUST_BUNDLE_MAP_NAME;
	}

}
