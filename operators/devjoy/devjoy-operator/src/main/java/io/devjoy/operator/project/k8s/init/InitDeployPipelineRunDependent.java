package io.devjoy.operator.project.k8s.init;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryStatus;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependent;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.api.model.PersistentVolumeClaimVolumeSourceBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
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
public class InitDeployPipelineRunDependent extends KubernetesDependentResource<io.fabric8.tekton.v1.PipelineRun, Project> implements Creator<PipelineRun, Project>, GarbageCollected<Project> {
	private static final Logger LOG = LoggerFactory.getLogger(InitDeployPipelineRunDependent.class);
	private boolean preferAdminAsGitUser = false;
	
	@Inject
	TektonClient tektonClient;
	@Inject
	OpenShiftClient ocpClient;
	
	
	public InitDeployPipelineRunDependent() {
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
		LOG.info("Run {} will be started in namespace {}", name, pipelineRun.getMetadata().getNamespace());
		pipelineRun.getSpec().setPipelineRef(new PipelineRefBuilder().withName(pipelineRun.getSpec().getPipelineRef().getName() + devEnvironment.getMetadata().getName()).build());
		LOG.info("Defining run {} referencing pipeline {}", name, pipelineRun.getSpec().getPipelineRef().getName());
		setRepoUrl("git_url", primary, context, pipelineRun, "gitopsRepository");
		setRepoUrl("git_src_url", primary, context, pipelineRun, "sourceRepository");
		String user = primary.getSpec().getOwner().getUser();
		
		boolean deprecatedUserSecretAvailable = context.getClient().secrets().inNamespace(devEnvironment.getMetadata().getNamespace())
			.withName(user + "-git-secret").get() != null; 
		
		Gitea gitea = GiteaDependentResource.getResource(context.getClient(), devEnvironment).get();
		pipelineRun.getSpec().getParams()
		.add(new ParamBuilder().withName("git_user").withNewValue(preferAdminAsGitUser ? gitea.getSpec().getAdminConfig().getAdminUser(): user).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_user_email").withNewValue(getUserEmailOrDefault(primary,preferAdminAsGitUser,gitea)).build());
        pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("project_name").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("project_namespace").withNewValue(primary.getMetadata().getNamespace()).build());
		if (primary.getSpec().getQuarkus() != null && primary.getSpec().getQuarkus().isEnabled()) {
			LOG.debug("We have a Quarkus project");
			pipelineRun.getSpec().setStatus("PipelineRunPending");
			pipelineRun.getSpec().getParams()
				.add(new ParamBuilder().withName("project_type").withNewValue("quarkus").build());
		}

		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("environment_namespace").withNewValue(devEnvironment.getMetadata().getNamespace()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("service_port").withNewValue("8080").build());
		
		String baseDomain = ocpClient.config().ingresses().withName("cluster").get().getSpec().getDomain();
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("route_host").withNewValue(String.format("%s-%s.%s", primary.getMetadata().getName(), primary.getMetadata().getNamespace(), baseDomain)).build());
		
		Optional<GiteaRepository> gitopsRepo = context.getSecondaryResource(GiteaRepository.class, "gitopsRepository");
		gitopsRepo.ifPresent(r -> pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_repository").withNewValue(r.getStatus().getInternalCloneUrl()).build()));

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

		return pipelineRun;
	}

	private void setRepoUrl(String param, Project primary, Context<Project> context, PipelineRun pipelineRun, String discrimnator) {
		Optional.ofNullable(primary.getSpec().getExistingRepositoryCloneUrl())
				.filter(url -> !StringUtil.isNullOrEmpty(url))
				.or(() -> context.getSecondaryResource(GiteaRepository.class, discrimnator)
				.map(GiteaRepository::getStatus)
						.map(GiteaRepositoryStatus::getCloneUrl))
				.map(url -> pipelineRun.getSpec().getParams()
						.add(new ParamBuilder().withName(param).withNewValue(url).build()))
				.orElseThrow(() -> new IllegalStateException("Git url is not yet available"));
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
		return "init-deploy-" + primary.getMetadata().getName();
	}

	private static PipelineRun getPipelineRunFromYaml(TektonClient tektonClient) {
		return tektonClient.v1()
				.pipelineRuns()
				.load(InitPipelineRunDependent.class.getClassLoader().getResourceAsStream("deploy/init-deploy-plr.yaml"))
				.item();
	}
	
	public static Resource<PipelineRun> getResource(TektonClient tektonClient, KubernetesClient client, Project primary) {
		return tektonClient.v1()
				.pipelineRuns()
				.inNamespace(getOwningEnvironment(primary, client).map(env -> env.getMetadata().getNamespace()).orElseGet(() -> primary.getMetadata().getNamespace()))
				.withName(getName(primary));
	}
	
	private static Optional<DevEnvironment> getOwningEnvironment(Project owningProject, KubernetesClient client) {
		return Optional.ofNullable(
				client.resources(DevEnvironment.class).inNamespace(owningProject.getSpec().getEnvironmentNamespace())
						.withName(owningProject.getSpec().getEnvironmentName()).get());
	}
}
