package io.devjoy.operator.project.k8s;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import io.devjoy.operator.repository.k8s.Repository;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent
public class PipelineRunDependentResource extends CRUDKubernetesDependentResource<PipelineRun, Project>{
	
	@Inject
	TektonClient tektonClient;
	
	public PipelineRunDependentResource() {
		super(PipelineRun.class);
	}

	@Override
	protected PipelineRun desired(Project primary, Context<Project> context) {
		PipelineRun pipelineRun = tektonClient.v1beta1()
				.pipelineRuns()
				.load(getClass().getClassLoader().getResourceAsStream("init/init-project-plr.yaml"))
				.get();
		String name = pipelineRun.getMetadata().getName() + primary.getMetadata().getName();
		pipelineRun.getMetadata().setName(name);
		pipelineRun.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		pipelineRun.getSpec().setPipelineRef(new PipelineRefBuilder().withName(pipelineRun.getSpec().getPipelineRef().getName() + primary.getMetadata().getName()).build());
		String cloneUrl = primary.getSpec().getExistingRepositoryCloneUrl();
		if (StringUtil.isNullOrEmpty(cloneUrl)) {
			cloneUrl = client.resources(Repository.class)
					.inNamespace(primary.getMetadata().getNamespace())
					.withName(primary.getMetadata().getName())
					.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCloneUrl()), 1, TimeUnit.MINUTES)
					.getStatus().getCloneUrl();
		}
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_url").withNewValue(cloneUrl).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user").withNewValue("user-1").build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user_email").withNewValue("user-1@example.com").build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_group_id").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_artifact_id").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "additional-resources".equals(w.getName()))
			.forEach(w -> w.getConfigMap().setName(w.getConfigMap().getName() + primary.getMetadata().getName()));
		
		return pipelineRun;
	}
}
