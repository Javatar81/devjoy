package io.devjoy.operator.environment.k8s.init;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class InitPipelineDiscriminator extends ResourceIDMatcherDiscriminator<Pipeline, DevEnvironment> {
    
	public InitPipelineDiscriminator() {
		super(p -> new ResourceID(InitPipelineDependent.getName(p), p.getMetadata().getNamespace()));
	}
    
}
