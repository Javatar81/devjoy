package io.devjoy.operator.environment.k8s.deploy;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class AdditionalDeployResourcesConfigmapDiscriminator extends ResourceIDMatcherDiscriminator<ConfigMap, DevEnvironment> {
    
	public AdditionalDeployResourcesConfigmapDiscriminator() {
		super(p -> new ResourceID(AdditionalDeployResourcesConfigmapDependent.getName(p), p.getMetadata().getNamespace()));
	}
   
}
