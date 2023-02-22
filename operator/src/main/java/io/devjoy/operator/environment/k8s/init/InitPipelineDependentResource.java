package io.devjoy.operator.environment.k8s.init;

import java.util.HashMap;

import javax.inject.Inject;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = InitPipelineDependentResource.LABEL_TYPE_SELECTOR)
public class InitPipelineDependentResource extends CRUDKubernetesDependentResource<Pipeline, DevEnvironment>{
	public static final String LABEL_KEY = "devjoy.io/pipeline.type";
	public static final String LABEL_VALUE = "init";
	static final String LABEL_TYPE_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	@Inject
	TektonClient tektonClient;
	
	public InitPipelineDependentResource() {
		super(Pipeline.class);
	}
	
	@Override
	protected Pipeline desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Pipeline pipeline = tektonClient.v1beta1()
				.pipelines()
				.load(getClass().getClassLoader().getResourceAsStream("init/init-project-pipe.yaml"))
				.get();
		String name = pipeline.getMetadata().getName() + primary.getMetadata().getName();
		pipeline.getMetadata().setName(name);
		pipeline.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		pipeline.getMetadata().setLabels(labels);
		return pipeline;
	}

}
