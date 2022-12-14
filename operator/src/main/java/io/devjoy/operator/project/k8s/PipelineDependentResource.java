package io.devjoy.operator.project.k8s;

import javax.inject.Inject;

import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class PipelineDependentResource extends CRUDKubernetesDependentResource<Pipeline, Project>{
	@Inject
	TektonClient tektonClient;
	
	public PipelineDependentResource() {
		super(Pipeline.class);
	}
	
	@Override
	protected Pipeline desired(Project primary, Context<Project> context) {
		Pipeline pipeline = tektonClient.v1beta1()
				.pipelines()
				.load(getClass().getClassLoader().getResourceAsStream("init/init-project-pipe.yaml"))
				.get();
		String name = pipeline.getMetadata().getName() + primary.getMetadata().getName();
		pipeline.getMetadata().setName(name);
		pipeline.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return pipeline;
	}

}
