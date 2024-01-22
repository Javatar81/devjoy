package io.devjoy.operator.project.k8s;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaUserSecretDependentResource;
import io.devjoy.operator.environment.k8s.deploy.ArgoActivationCondition;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.environment.k8s.PipelineActivationCondition;
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.TriggerBindingActivationCondition;
import io.devjoy.operator.environment.k8s.build.TriggerTemplateActivationCondition;
import io.devjoy.operator.project.k8s.deploy.Application;
import io.devjoy.operator.project.k8s.deploy.ApplicationActivationCondition;
import io.devjoy.operator.project.k8s.deploy.ApplicationDependentResource;
import io.devjoy.operator.project.k8s.deploy.ApplicationReconcileCondition;
import io.devjoy.operator.project.k8s.deploy.GitopsRepositoryDependentResource;
import io.devjoy.operator.project.k8s.init.InitDeployPipelineRunDependentResource;
import io.devjoy.operator.project.k8s.init.InitPipelineRunDependentResource;
import io.devjoy.operator.project.k8s.init.PipelineRunActivationCondition;
import io.fabric8.kubernetes.api.model.Condition;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.PipelineRun;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusHandler;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(dependents = { @Dependent(type = SourceRepositoryDependentResource.class),
		@Dependent(type = GitopsRepositoryDependentResource.class),
		@Dependent(reconcilePrecondition = ApplicationReconcileCondition.class, activationCondition = ApplicationActivationCondition.class,type = ApplicationDependentResource.class),
		@Dependent(activationCondition = PipelineRunActivationCondition.class, type = InitPipelineRunDependentResource.class),
		@Dependent(activationCondition = PipelineRunActivationCondition.class, type = InitDeployPipelineRunDependentResource.class)
		})
public class ProjectReconciler implements Reconciler<Project>, ErrorStatusHandler<Project>, Cleaner<Project> {
	private static final Logger LOG = LoggerFactory.getLogger(ProjectReconciler.class);
	private final OpenShiftClient client;
	private final TektonClient tektonClient;
	private final SourceRepositoryDiscriminator sourceRepoDiscriminator = new SourceRepositoryDiscriminator();

	public ProjectReconciler(OpenShiftClient client, TektonClient tektonClient) {
		this.client = client;
		this.tektonClient = tektonClient;
	}

	@Override
	public UpdateControl<Project> reconcile(Project resource, Context<Project> context) {
		LOG.info("Reconcile");
		if (resource.getStatus() == null) {
			ProjectStatus status = new ProjectStatus();
			resource.setStatus(status);
		}
		
		Optional<DevEnvironment> owningEnvironment = resource.getOwningEnvironment(client);
		owningEnvironment.orElseThrow(() -> new EnvironmentNotFoundException("Environment cannot be found", resource) );
		owningEnvironment
			.filter(env -> !resource.getMetadata().getNamespace().equals(env.getMetadata().getNamespace()))
			.ifPresent(env -> {
				LOG.info("Project is in different namespace than its environment");
				allowProjectNamespaceToPullImagesFromEnvNamespace(resource, env);
				makeArgoManageProjectNamespace(resource, env);
			});
		return owningEnvironment.flatMap(e -> {

			Optional<Secret> secret = Optional.ofNullable(GiteaDependentResource.getResource(client, e).get())
				.flatMap(g -> Optional.ofNullable(GiteaUserSecretDependentResource.getResource(g, resource.getSpec().getOwner().getUser(), client).get()));
			
			return secret.map(st -> {
				UpdateControl<Project> ctrl = UpdateControl.noUpdate();
				if (resource.getStatus().getRepository() == null || !resource.getStatus().getRepository().isUserSecretAvailable()) {
					resource.getStatus().getRepository().setUserSecretAvailable(true);
					ctrl = UpdateControl.patchStatus(resource);
				}
				
				if ((resource.getStatus().getWorkspace() == null 
					|| StringUtil.isNullOrEmpty(resource.getStatus().getWorkspace().getFactoryUrl())) 
					&& supportsRequiredPipelinesApi()) {
					LOG.info("Setting workspace factory url");
					PipelineRun pipelineRun = InitPipelineRunDependentResource.getResource(tektonClient, resource)
						.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCompletionTime()), 10, TimeUnit.MINUTES);
						onPipelineRunComplete(pipelineRun, resource, context);
					ctrl = UpdateControl.patchStatus(resource);
				}

				if (!supportsRequiredPipelinesApi() 
					&& !resource.getStatus().getConditions().stream().anyMatch(c -> ProjectConditionType.PIPELINES_API_UNAVAILABLE.toString().equals(c.getType()))) {
					Condition noPipelinesApi = new ConditionBuilder()
						.withObservedGeneration(resource.getStatus().getObservedGeneration())
						.withType(ProjectConditionType.PIPELINES_API_UNAVAILABLE.toString())
						.withMessage("Error")
						.withLastTransitionTime(LocalDateTime.now().toString())
						.withReason("Required api for Tekton not available. Is OpenShift Pipelines installed?")
						.withStatus("false")
						.build();
					resource.getStatus().getConditions().add(noPipelinesApi);
					resource.getStatus().getInitStatus().setMessage("Required api for Tekton not available.");
					ctrl = UpdateControl.patchStatus(resource);
				} else if (supportsRequiredPipelinesApi()) {
					resource.getStatus().getInitStatus().setMessage("Pipelines Api available");
				}

				if (!supportsRequiredGitopsApi()
					&& !resource.getStatus().getConditions().stream().anyMatch(c -> ProjectConditionType.GITOPS_API_UNAVAILABLE.toString().equals(c.getType()))) {
					Condition noGitopsCondition = new ConditionBuilder()
						.withObservedGeneration(resource.getStatus().getObservedGeneration())
						.withType(ProjectConditionType.GITOPS_API_UNAVAILABLE.toString())
						.withMessage("Error")
						.withLastTransitionTime(LocalDateTime.now().toString())
						.withReason("Required api for ArgoCD or Application not found. Is OpenShift Gitops installed?")
						.withStatus("false")
						.build();
					resource.getStatus().getConditions().add(noGitopsCondition);
					resource.getStatus().getDeployStatus().setMessage("Required api for ArgoCD or Application not found");
					ctrl = UpdateControl.patchStatus(resource);
				} else if (supportsRequiredPipelinesApi()) {
					resource.getStatus().getDeployStatus().setMessage("GitOps Api available");
				}
				return ctrl;
			});
		}).orElseGet(UpdateControl::noUpdate);
	}

	private boolean supportsRequiredPipelinesApi() {
		return PipelineActivationCondition.serverSupportsApi(client) 
		&& PipelineRunActivationCondition.serverSupportsApi(client)
		&& TriggerBindingActivationCondition.serverSupportsApi(client)
		&& EventListenerActivationCondition.serverSupportsApi(client)
		&& TriggerTemplateActivationCondition.serverSupportsApi(client);
	}

	private boolean supportsRequiredGitopsApi() {
		return ArgoActivationCondition.serverSupportsApi(client)
		&& ApplicationActivationCondition.serverSupportsApi(client);
	}

	@Override
	public DeleteControl cleanup(Project resource, Context<Project> context) {
		// We need to clean up application because it has no owner because environment and project could be in different namespaces
		LOG.info("Deleting project {}. Making sure that application will be deleted since it is not owned by project.", resource.getMetadata().getName());
		if (supportsRequiredGitopsApi()) {
			context.getSecondaryResource(Application.class).ifPresent(a -> client.resource(a).delete());
		}
		return DeleteControl.defaultDelete();
	}

	@Override
	public ErrorStatusUpdateControl<Project> updateErrorStatus(Project project, Context<Project> context, Exception e) {
		LOG.info("Error of type {}", e.getClass());
		if (e.getCause() instanceof EnvironmentNotFoundException) {
			EnvironmentNotFoundException envNotFoundException = (EnvironmentNotFoundException) e.getCause();
			project.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(project.getStatus().getObservedGeneration())
					.withType(ProjectConditionType.ENV_NOT_FOUND.toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(envNotFoundException.getMessage())
					.withStatus("false")
					.build());
		}
		return ErrorStatusUpdateControl.patchStatus(project);
	}


	private void allowProjectNamespaceToPullImagesFromEnvNamespace(Project primary, DevEnvironment env) {
		String roleBindingName = primary.getMetadata().getNamespace() + "-imagepuller";
		String roleBindingNamespace = env.getMetadata().getNamespace();
		Resource<RoleBinding> cr = client.rbac().roleBindings().inNamespace(roleBindingNamespace).withName(roleBindingName);
		LOG.info("Allow all service accounts in {} to pull images from {}", primary.getMetadata().getNamespace(), roleBindingNamespace);
		if (cr.get() == null) {
			RoleBinding roleBinding = new RoleBindingBuilder().withNewMetadata().withName(roleBindingName).withNamespace(roleBindingNamespace).endMetadata()
				.addNewSubject().withApiGroup("rbac.authorization.k8s.io").withKind("Group").withName("system:serviceaccounts:" + primary.getMetadata().getNamespace()).endSubject()
				.withNewRoleRef("rbac.authorization.k8s.io", "ClusterRole", "system:image-puller")
				.build();
			client.resource(roleBinding).create();
			LOG.info("Created role binding");
		}
	}

	private void makeArgoManageProjectNamespace(Project primary, DevEnvironment env) {
		Optional<Namespace> projectNamespace = Optional.ofNullable(client.namespaces().withName(primary.getMetadata().getNamespace()).get());
		LOG.info("Making sure namespace {} is managed by argo", primary.getMetadata().getNamespace());
		projectNamespace
			.filter(n -> !env.getMetadata().getNamespace().equals(n.getMetadata().getLabels().get("argocd.argoproj.io/managed-by")))
			.ifPresent(n -> {
				LOG.info("Adding label for managing namespace {} by argo", n.getMetadata().getName());
				client.resource(n).edit(ne -> n.edit().editMetadata().addToLabels("argocd.argoproj.io/managed-by", env.getMetadata().getNamespace()).endMetadata().build());
			});
	}

	private void onPipelineRunComplete(PipelineRun pipelineRun, Project resource, Context<Project> context) {
		resource.getStatus().getInitStatus().setPipelineRunConditions(pipelineRun.getStatus().getConditions());
		resource.getStatus().getInitStatus().setMessage("Init pipeline run complete.");
		Optional<GiteaRepository> repository = context.getSecondaryResource(GiteaRepository.class, sourceRepoDiscriminator);
		repository.ifPresent(r -> {
			String devFilePath = r.getStatus().getInternalCloneUrl().replace(".git", "/raw/branch/main/devfile.yaml");
			resource.getStatus().getWorkspace().setFactoryUrl(String.format("%s#%s", getDevSpacesUrl(), devFilePath));
		});
		
	}
	
	private String getDevSpacesUrl() {
		return client.getOpenshiftUrl().getProtocol() + 
				"://" +
				client.getOpenshiftUrl().getHost().replace("api.", "devspaces.apps.");
	}
}
