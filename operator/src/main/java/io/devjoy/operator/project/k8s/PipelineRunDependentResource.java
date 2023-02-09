package io.devjoy.operator.project.k8s;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.fabric8.kubernetes.client.dsl.Resource;
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
		PipelineRun pipelineRun = getPipelineRunFromYaml(tektonClient);
		String name = getName(primary, pipelineRun);
		pipelineRun.getMetadata().setName(name);
		pipelineRun.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		pipelineRun.getSpec().setPipelineRef(new PipelineRefBuilder().withName(pipelineRun.getSpec().getPipelineRef().getName() + primary.getMetadata().getName()).build());
		String cloneUrl = primary.getSpec().getExistingRepositoryCloneUrl();
		if (StringUtil.isNullOrEmpty(cloneUrl)) {
			cloneUrl = client.resources(GiteaRepository.class)
					.inNamespace(primary.getMetadata().getNamespace())
					.withName(primary.getMetadata().getName())
					.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCloneUrl()), 1, TimeUnit.MINUTES)
					.getStatus().getCloneUrl();
		}
		String user = primary.getSpec().getOwner().getUser();
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_url").withNewValue(cloneUrl).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user").withNewValue(user).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user_email").withNewValue(getUserEmailOrDefault(primary)).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_group_id").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_artifact_id").withNewValue(primary.getMetadata().getName()).build());
		
		// E.g. quarkus-resteasy-reactive quarkus-reactive-routes
		Optional.ofNullable(primary.getSpec().getQuarkus())
			.map(q -> q.getExtensions().stream().collect(Collectors.joining(" ")))
			.ifPresent(ext -> pipelineRun.getSpec().getParams()
					.add(new ParamBuilder().withName("quarkus_extensions")
							.withNewValue(ext)
									.build()));
		
		
		
		
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "auth".equals(w.getName()))
			.forEach(w -> w.getSecret().setSecretName(user + w.getSecret().getSecretName()));
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "additional-resources".equals(w.getName()))
			.forEach(w -> w.getConfigMap().setName(w.getConfigMap().getName() + primary.getMetadata().getName()));
		return pipelineRun;
	}

	private String getUserEmailOrDefault(Project primary) {
		if (primary.getSpec().getOwner() != null && primary.getSpec().getOwner().getUserEmail() != null) {
			return primary.getSpec().getOwner().getUserEmail();
		} else {
			return primary.getSpec().getOwner().getUser() + "@example.com";
		}
	}
	
	private static String getName(Project primary, PipelineRun pipelineRun) {
		return pipelineRun.getMetadata().getName() + primary.getMetadata().getName();
	}

	private static PipelineRun getPipelineRunFromYaml(TektonClient tektonClient) {
		return tektonClient.v1beta1()
				.pipelineRuns()
				.load(PipelineRunDependentResource.class.getClassLoader().getResourceAsStream("init/init-project-plr.yaml"))
				.get();
	}
	
	public static Resource<PipelineRun> getResource(TektonClient tektonClient, Project primary) {
		PipelineRun run = getPipelineRunFromYaml(tektonClient);
		return tektonClient.v1beta1()
				.pipelineRuns()
				.inNamespace(primary.getMetadata().getNamespace())
				.withName(getName(primary, run));
	}
}
