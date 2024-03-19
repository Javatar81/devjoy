package io.devjoy.operator.environment.k8s.init;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class AdditionalResourcesConfigmapDiscriminator extends ResourceIDMatcherDiscriminator<ConfigMap, DevEnvironment> {
    

	public AdditionalResourcesConfigmapDiscriminator() {
		super(p -> new ResourceID(AdditionalResourcesConfigmapDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
