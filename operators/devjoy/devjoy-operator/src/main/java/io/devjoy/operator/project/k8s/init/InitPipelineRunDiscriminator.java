package io.devjoy.operator.project.k8s.init;

import io.devjoy.operator.project.k8s.Project;
import io.fabric8.tekton.pipeline.v1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.ResourceIDMatcherDiscriminator;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.quarkus.runtime.util.StringUtil;

public class InitPipelineRunDiscriminator extends ResourceIDMatcherDiscriminator<PipelineRun, Project> {
	
	public InitPipelineRunDiscriminator() {
		super(p -> new ResourceID(InitPipelineRunDependent.getName(p),
				!StringUtil.isNullOrEmpty(p.getSpec().getEnvironmentNamespace()) ? p.getSpec().getEnvironmentNamespace()
						: p.getMetadata().getNamespace()));
	}

}