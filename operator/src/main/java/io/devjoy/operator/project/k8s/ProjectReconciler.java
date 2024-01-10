package io.devjoy.operator.project.k8s;

import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaUserSecretDependentResource;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.project.k8s.deploy.Application;
import io.devjoy.operator.project.k8s.deploy.ApplicationDependentResource;
import io.devjoy.operator.project.k8s.deploy.ApplicationReconcileCondition;
import io.devjoy.operator.project.k8s.deploy.GitopsRepositoryDependentResource;
import io.devjoy.operator.project.k8s.init.InitDeployPipelineRunCondition;
import io.devjoy.operator.project.k8s.init.InitDeployPipelineRunDependentResource;
import io.devjoy.operator.project.k8s.init.InitPipelineRunCondition;
import io.devjoy.operator.project.k8s.init.InitPipelineRunDependentResource;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.StatusDetails;
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
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(dependents = { @Dependent(type = SourceRepositoryDependentResource.class),
		@Dependent(type = GitopsRepositoryDependentResource.class),
		@Dependent(reconcilePrecondition = ApplicationReconcileCondition.class, type = ApplicationDependentResource.class),
		@Dependent(/*name = ProjectReconciler.RESOURCE_NAME_INIT_PIPE,*/ type = InitPipelineRunDependentResource.class),
		@Dependent(/*name = ProjectReconciler.RESOURCE_NAME_INIT_DEPLOY_PIPE,*/ type = InitDeployPipelineRunDependentResource.class)
		})
public class ProjectReconciler implements Reconciler<Project>, Cleaner<Project> {
	private final OpenShiftClient client;
	private final TektonClient tektonClient;
	private final SourceRepositoryDiscriminator sourceRepoDiscriminator = new SourceRepositoryDiscriminator();
	private static final Logger LOG = LoggerFactory.getLogger(ProjectReconciler.class);
	public static final String RESOURCE_NAME_INIT_DEPLOY_PIPE = "InitDeployPipe";
	public static final String RESOURCE_NAME_INIT_PIPE = "InitPipe"; 

	public ProjectReconciler(OpenShiftClient client, TektonClient tektonClient) {
		this.client = client;
		this.tektonClient = tektonClient;
	}

	@Override
	public UpdateControl<Project> reconcile(Project resource, Context<Project> context) {
		LOG.info("Reconcile");
		
		if (resource.getStatus() == null) {
			resource.setStatus(new ProjectStatus());
		}
		Optional<DevEnvironment> owningEnvironment = resource.getOwningEnvironment(client);
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
					RepositoryStatus repositoryStatus = new RepositoryStatus();
					repositoryStatus.setUserSecretAvailable(true);
					resource.getStatus().setRepository(repositoryStatus);
					ctrl = UpdateControl.patchStatus(resource);
				}
				
				if (resource.getStatus().getWorkspace() == null || StringUtil.isNullOrEmpty(resource.getStatus().getWorkspace().getFactoryUrl())) {
					LOG.info("Setting workspace factory url");
					PipelineRun pipelineRun = InitPipelineRunDependentResource.getResource(tektonClient, resource)
					.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCompletionTime()), 10, TimeUnit.MINUTES);
					onPipelineRunComplete(pipelineRun, resource, context);
					ctrl = UpdateControl.patchStatus(resource);
				}
				return ctrl;
			});
		}).orElseGet(UpdateControl::noUpdate);
	}

	@Override
	public DeleteControl cleanup(Project resource, Context<Project> context) {
		// We need to clean up application because it has no owner because environment and project could be in different namespaces
		LOG.info("Deleting project {}. Making sure that application will be deleted since it is not owned by project.", resource.getMetadata().getName());
		context.getSecondaryResource(Application.class).ifPresent(a -> client.resource(a).delete());
		return DeleteControl.defaultDelete();
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
		WorkspaceStatus status = new WorkspaceStatus();
		InitStatus initStatus = new InitStatus();
		initStatus.setPipelineRunConditions(pipelineRun.getStatus().getConditions());
		status.setInitStatus(initStatus);
		Optional<GiteaRepository> repository = context.getSecondaryResource(GiteaRepository.class, sourceRepoDiscriminator);
		repository.ifPresent(r -> {
			String devFilePath = r.getStatus().getInternalCloneUrl().replace(".git", "/raw/branch/main/devfile.yaml");
			status.setFactoryUrl(String.format("%s#%s", getDevSpacesUrl(), devFilePath));
			resource.getStatus().setWorkspace(status);
		});
		
	}
	
	private String getDevSpacesUrl() {
		return client.getOpenshiftUrl().getProtocol() + 
				"://" +
				client.getOpenshiftUrl().getHost().replace("api.", "devspaces.apps.");
	}
}
