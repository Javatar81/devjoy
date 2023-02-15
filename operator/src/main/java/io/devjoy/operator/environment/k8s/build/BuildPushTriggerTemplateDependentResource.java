package io.devjoy.operator.environment.k8s.build;

import java.util.Optional;

import javax.inject.Inject;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.fabric8.tekton.triggers.v1alpha1.TriggerTemplate;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class BuildPushTriggerTemplateDependentResource extends CRUDKubernetesDependentResource<TriggerTemplate, DevEnvironment>{
	@Inject
	TektonClient tektonClient;
	
	public BuildPushTriggerTemplateDependentResource() {
		super(TriggerTemplate.class);
	}

	@Override
	protected TriggerTemplate desired(DevEnvironment primary, Context<DevEnvironment> context) {
		TriggerTemplate triggerTemplate = tektonClient.v1alpha1()
				.triggerTemplates()
				.load(getClass().getClassLoader().getResourceAsStream("build/build-push-trigger-template.yaml"))
				.get();
		triggerTemplate.getMetadata().setName(getName(primary));
		triggerTemplate.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		
		Optional<PipelineRun> resourceTemplate = triggerTemplate.getSpec().getResourcetemplates()
				.stream().map(PipelineRun.class::cast).findAny();
		resourceTemplate.ifPresent(p -> {
			p.getMetadata().setGenerateName(p.getMetadata().getGenerateName() + primary.getMetadata().getName() + "-");
			p.getMetadata().getLabels().put("tekton.dev/pipeline", p.getMetadata().getLabels().get("tekton.dev/pipeline") + primary.getMetadata().getName());
			p.getMetadata().setNamespace(primary.getMetadata().getNamespace());
			p.getSpec().getParams().add(new ParamBuilder()
					.withName("image_url")
					.withNewValue(String.format("image-registry.openshift-image-registry.svc:5000/%s/$(tt.params.git-repo-name)", primary.getMetadata().getNamespace()))
					.build()
			);
			p.getSpec().getPipelineRef().setName(p.getSpec().getPipelineRef().getName() + primary.getMetadata().getName());
		});
		//TODO Set Pipeline in resourcetemplates
		//TODO Set Pipeline namespace in resourcetemplates
		//TODO Set image_url in resourcetemplates
		//TODO Set Pipeline ref in resourcetemplates
		return triggerTemplate;
	}
	
	public static String getName(DevEnvironment primary) {
		return "build-push-template-" + primary.getMetadata().getName();
	}

}
