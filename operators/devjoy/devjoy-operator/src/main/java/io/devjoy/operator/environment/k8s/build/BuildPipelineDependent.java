package io.devjoy.operator.environment.k8s.build;

import java.util.HashMap;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.Pipeline;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent(resourceDiscriminator = BuildPipelineDiscriminator.class, labelSelector = BuildPipelineDependent.LABEL_TYPE_SELECTOR)
public class BuildPipelineDependent extends CRUDKubernetesDependentResource<Pipeline, DevEnvironment>{
	public static final String LABEL_KEY = "devjoy.io/pipeline.type";
	public static final String LABEL_VALUE = "build";
	static final String LABEL_TYPE_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	@Inject
	TektonClient tektonClient;
	
	public BuildPipelineDependent() {
		super(Pipeline.class);
	}
	
	@Override
	protected Pipeline desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Pipeline pipeline = tektonClient.v1()
				.pipelines()
				.load(getClass().getClassLoader().getResourceAsStream("build/build-pipe.yaml"))
				.item();
		String name = getName(primary);
		pipeline.getMetadata().setName(name);
		pipeline.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		pipeline.getMetadata().setLabels(labels);
		return pipeline;
	}

	static String getName(DevEnvironment primary) {
		return "build-project-" + primary.getMetadata().getName();
	}

}