package io.devjoy.operator.project.k8s.init;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.project.k8s.Project;
import io.devjoy.operator.project.k8s.deploy.GitopsRepositoryDependent;
import io.devjoy.operator.project.k8s.deploy.GitopsRepositoryDiscriminator;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.ParamBuilder;
import io.fabric8.tekton.pipeline.v1.PipelineRefBuilder;
import io.fabric8.tekton.pipeline.v1.PipelineRun;
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
@KubernetesDependent(resourceDiscriminator = InitDeployPipelineRunDiscriminator.class)
public class InitDeployPipelineRunDependent extends KubernetesDependentResource<PipelineRun, Project> implements Creator<PipelineRun, Project>, GarbageCollected<Project> {
	private static final Logger LOG = LoggerFactory.getLogger(InitDeployPipelineRunDependent.class);
	private GitopsRepositoryDiscriminator gitopsRepoDiscriminator = new GitopsRepositoryDiscriminator();
	
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
		String cloneUrl = primary.getSpec().getExistingRepositoryCloneUrl();
		if (StringUtil.isNullOrEmpty(cloneUrl)) {
			cloneUrl = context.getClient().resources(GiteaRepository.class)
					.inNamespace(primary.getMetadata().getNamespace())
					.withName(primary.getMetadata().getName() + GitopsRepositoryDependent.REPO_POSTFIX)
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
			.add(new ParamBuilder().withName("project_name").withNewValue(primary.getMetadata().getName()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("project_namespace").withNewValue(primary.getMetadata().getNamespace()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("environment_namespace").withNewValue(devEnvironment.getMetadata().getNamespace()).build());
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("service_port").withNewValue("8080").build());
		
		String baseDomain = ocpClient.config().ingresses().withName("cluster").get().getSpec().getDomain();
		pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("route_host").withNewValue(String.format("%s-%s.%s", primary.getMetadata().getName(), primary.getMetadata().getNamespace(), baseDomain)).build());
		
		Optional<GiteaRepository> gitopsRepo = Optional.ofNullable(context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator).get());
		gitopsRepo.ifPresent(r -> pipelineRun.getSpec().getParams()
			.add(new ParamBuilder().withName("git_repository").withNewValue(r.getStatus().getInternalCloneUrl()).build()));

		boolean deprecatedUserSecretAvailable = context.getClient().secrets().inNamespace(devEnvironment.getMetadata().getNamespace())
			.withName(user + "-git-secret").get() != null; 
		String secretPrefix;
		if (deprecatedUserSecretAvailable){
			secretPrefix = user;
		} else {
			secretPrefix = GiteaDependentResource.getResource(context.getClient(), devEnvironment).get().getSpec().getAdminUser();
		}
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "auth".equals(w.getName()))
			.forEach(w -> w.getSecret().setSecretName(secretPrefix + "-git-secret"));

			
		pipelineRun.getSpec().getWorkspaces().stream()
			.filter(w -> "additional-resources".equals(w.getName()))
			.forEach(w -> w.getConfigMap().setName(w.getConfigMap().getName() + devEnvironment.getMetadata().getName()));

		return pipelineRun;
	}

	private String getUserEmailOrDefault(Project primary) {
		if (primary.getSpec().getOwner() != null && primary.getSpec().getOwner().getUserEmail() != null) {
			return primary.getSpec().getOwner().getUserEmail();
		} else {
			return primary.getSpec().getOwner().getUser() + "@example.com";
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
