package io.devjoy.operator.environment.k8s.build;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class BuildPipelineDiscriminator extends ResourceIDMatcherDiscriminator<Pipeline, DevEnvironment> {

	public BuildPipelineDiscriminator() {
		super(p -> new ResourceID(BuildPipelineDependent.getName(p), p.getMetadata().getNamespace()));
	}
	
}
