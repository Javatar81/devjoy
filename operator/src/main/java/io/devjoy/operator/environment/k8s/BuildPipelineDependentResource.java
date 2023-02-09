package io.devjoy.operator.environment.k8s;

import javax.inject.Inject;

import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class BuildPipelineDependentResource extends CRUDKubernetesDependentResource<Pipeline, DevEnvironment>{
	
	@Inject
	TektonClient tektonClient;
	
	public BuildPipelineDependentResource() {
		super(Pipeline.class);
	}
	
	@Override
	protected Pipeline desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Pipeline pipeline = tektonClient.v1beta1()
				.pipelines()
				.load(getClass().getClassLoader().getResourceAsStream("build/build-pipe.yaml"))
				.get();
		String name = pipeline.getMetadata().getName() + primary.getMetadata().getName();
		pipeline.getMetadata().setName(name);
		pipeline.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return pipeline;
	}

}