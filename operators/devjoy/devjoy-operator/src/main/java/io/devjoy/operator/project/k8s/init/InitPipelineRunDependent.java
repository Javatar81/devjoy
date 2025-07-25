package io.devjoy.operator.project.k8s.init;

import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryStatus;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.v1.ParamBuilder;
import io.fabric8.tekton.v1.ParamValue;
import io.fabric8.tekton.v1.ParamValueBuilder;
import io.fabric8.tekton.v1.PipelineRefBuilder;
import io.fabric8.tekton.v1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.GarbageCollected;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.quarkus.runtime.util.StringUtil;
import jakarta.inject.Inject;

/**
 * 
 * This resource is not garbage collected and thus will not use an owner reference
 *
 */
@KubernetesDependent
public class InitPipelineRunDependent extends KubernetesDependentResource<PipelineRun, Project> implements Creator<PipelineRun, Project>, GarbageCollected<Project>{
	private static final Logger LOG = LoggerFactory.getLogger(InitPipelineRunDependent.class);
	private boolean preferAdminAsGitUser = false;

	@Inject
	TektonClient tektonClient;
	
	public InitPipelineRunDependent() {
		super(PipelineRun.class);
	}
	
	@Override
	protected PipelineRun desired(Project primary, Context<Project> context) {
		LOG.info("Setting desired state for pipeline run");
		PipelineRun pipelineRun = getPipelineRunFromYaml(tektonClient);
		String name = getName(primary);
		pipelineRun.getMetadata().setName(name);
		
		DevEnvironment devEnvironment = primary.getOwningEnvironment(context.getClient()).get();
		pipelineRun.getMetadata().setNamespace(devEnvironment.getMetadata().getNamespace());
		pipelineRun.getSpec().setPipelineRef(new PipelineRefBuilder().withName(pipelineRun.getSpec().getPipelineRef().getName() + devEnvironment.getMetadata().getName()).build());
		LOG.info("Defining desired run {} referencing {}", name, pipelineRun.getSpec().getPipelineRef().getName());
		Optional.ofNullable(primary.getSpec().getExistingRepositoryCloneUrl())
			.filter(url -> !StringUtil.isNullOrEmpty(url))
			.or(() -> context.getSecondaryResource(GiteaRepository.class, "sourceRepository").map(GiteaRepository::getStatus)
					.map(GiteaRepositoryStatus::getCloneUrl))
			.map(url -> pipelineRun.getSpec().getParams()
					.add(new ParamBuilder().withName("git_url").withNewValue(url).build()))
			.orElseThrow(() -> new IllegalStateException("Git url is not yet available"));
		String user = primary.getSpec().getOwner().getUser();

		boolean deprecatedUserSecretAvailable = context.getClient().secrets().inNamespace(devEnvironment.getMetadata().getNamespace())
			.withName(user + "-git-secret").get() != null; 
		Gitea gitea = GiteaDependentResource.getResource(context.getClient(), devEnvironment).get();
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user").withNewValue(preferAdminAsGitUser ? gitea.getSpec().getAdminConfig().getAdminUser() : user).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user_email").withNewValue(getUserEmailOrDefault(primary, preferAdminAsGitUser, gitea)).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_group_id").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("quarkus_artifact_id").withNewValue(primary.getMetadata().getName()).build());
		
		// E.g. quarkus-resteasy-reactive quarkus-reactive-routes
		Optional.ofNullable(primary.getSpec())
			.filter(p -> p.getQuarkus() != null)
			.map(s -> s.getQuarkus())
			.filter(q -> q.getExtensions() != null)
			.map(q -> q.getExtensions().stream().collect(Collectors.joining(",")))
			.ifPresent(ext -> pipelineRun.getSpec().getParams()
					.add(new ParamBuilder().withName("quarkus_extensions")
							.withNewValue(ext)
									.build()));
		
		
		String secretPrefix;
		if (deprecatedUserSecretAvailable){
			secretPrefix = user;
		} else {
			secretPrefix = gitea.getSpec().getAdminConfig().getAdminUser();
		}
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "auth".equals(w.getName()))
			.forEach(w -> w.getSecret().setSecretName(secretPrefix + "-git-secret"));

		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "additional-resources".equals(w.getName()))
			.forEach(w -> w.getConfigMap().setName(w.getConfigMap().getName() + devEnvironment.getMetadata().getName()));
		if (devEnvironment.getSpec().getMavenSettingsPvc() != null) {
			ParamValue mavenRepoPath = new ParamValueBuilder().withStringVal("/workspace/maven_settings/repo").build();
			pipelineRun.getSpec().getParams()
				.add(new ParamBuilder().withName("maven_repo").withValue(mavenRepoPath).build());
			pipelineRun.getSpec().getWorkspaces().stream()
				.filter(w -> "mvn-settings".equals(w.getName()))
				.forEach(w -> 
					{
						w.setEmptyDir(null);
						w.setPersistentVolumeClaim(new PersistentVolumeClaimVolumeSourceBuilder().withClaimName(devEnvironment.getSpec().getMavenSettingsPvc()).build());
					});
		}
		LOG.info("Pipeline run defined.");
		return pipelineRun;
	}

	private String getUserEmailOrDefault(Project primary, boolean preferAdminAsGitUser, Gitea gitea) {
		if(preferAdminAsGitUser) {
			if (gitea.getSpec().getAdminConfig().getAdminEmail() != null) {
				return gitea.getSpec().getAdminConfig().getAdminEmail();
			} else {
				return gitea.getSpec().getAdminConfig().getAdminUser()+ "@example.com";
			}
		} else {
			if (primary.getSpec().getOwner() != null && primary.getSpec().getOwner().getUserEmail() != null) {
				return primary.getSpec().getOwner().getUserEmail();
			} else {
				return primary.getSpec().getOwner().getUser() + "@example.com";
			}
			
		}
		
	}
	
	static String getName(Project primary) {
		return "init-project-" + primary.getMetadata().getName();
	}

	private static PipelineRun getPipelineRunFromYaml(TektonClient tektonClient) {
		return tektonClient.v1()
				.pipelineRuns()
				.load(InitPipelineRunDependent.class.getClassLoader().getResourceAsStream("init/init-project-plr.yaml"))
				.item();
	}
	
	public static Resource<PipelineRun> getResource(TektonClient tektonClient, Project primary) {
		return tektonClient.v1()
				.pipelineRuns()
				.inNamespace(primary.getMetadata().getNamespace())
				.withName(getName(primary));
	}
	
}
