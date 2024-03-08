package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class GiteaTrustMapDiscriminator extends ResourceIDMatcherDiscriminator<ConfigMap, Gitea> {
	public GiteaTrustMapDiscriminator() {
		super(p -> new ResourceID(GiteaTrustMapDependentResource.getName(p), p.getMetadata().getNamespace()));
	}

}
