package io.devjoy.operator.environment.k8s.build;

import java.util.Optional;


import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1.ParamValue;
import io.fabric8.tekton.pipeline.v1.ParamValueBuilder;
import io.fabric8.tekton.pipeline.v1.PipelineRun;
import io.fabric8.tekton.triggers.v1beta1.TriggerTemplate;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public class BuildPushTriggerTemplateDependent extends CRUDKubernetesDependentResource<TriggerTemplate, DevEnvironment>{
	@Inject
	TektonClient tektonClient;
	
	public BuildPushTriggerTemplateDependent() {
		super(TriggerTemplate.class);
	}

	@Override
	protected TriggerTemplate desired(DevEnvironment primary, Context<DevEnvironment> context) {
		TriggerTemplate triggerTemplate = tektonClient.v1beta1()
				.triggerTemplates()
				.load(getClass().getClassLoader().getResourceAsStream("build/build-push-trigger-template.yaml"))
				.item();
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
			if (primary.getSpec().getMavenSettingsPvc() != null) {
				ParamValue mavenRepoPath = new ParamValueBuilder().addToArrayVal("-Dmaven.repo.local=$(workspaces.maven-settings.path)").build();
				p.getSpec().getParams()
					.add(new ParamBuilder().withName("additional_maven_params").withValue(mavenRepoPath).build());
				p.getSpec().getWorkspaces().stream()
					.filter(w -> "mvn-settings".equals(w.getName()))
					.forEach(w -> 
						{
							w.setEmptyDir(null);
							w.setPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(primary.getSpec().getMavenSettingsPvc()).build());
						});
			}
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
