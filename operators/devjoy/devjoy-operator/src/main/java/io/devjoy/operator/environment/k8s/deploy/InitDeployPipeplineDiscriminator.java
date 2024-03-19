package io.devjoy.operator.environment.k8s.deploy;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class InitDeployPipeplineDiscriminator extends ResourceIDMatcherDiscriminator<Pipeline, DevEnvironment> {
    
	public InitDeployPipeplineDiscriminator() {
		super(p -> new ResourceID(InitDeployPipelineDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
