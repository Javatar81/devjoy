package io.devjoy.operator.environment.k8s.init;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.pipeline.v1.Task;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;

public class QuarkusTaskDiscriminator extends ResourceIDMatcherDiscriminator<Task, DevEnvironment> {
    
	public QuarkusTaskDiscriminator() {
		super(p -> new ResourceID("quarkus-create", p.getMetadata().getNamespace()));
	}
	
}
