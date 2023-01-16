package io.devjoy.operator.project.k8s;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaUserSecretDependentResource;
import io.devjoy.operator.environment.k8s.TaskDependentResource;
import io.devjoy.operator.environment.service.EnvironmentServiceImpl;
import io.devjoy.operator.repository.k8s.Repository;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.client.OpenShiftClient;
import io.fabric8.tekton.client.TektonClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkus.runtime.util.StringUtil;

@ControllerConfiguration(dependents = { @Dependent(type = RepositoryDependentResource.class),
		@Dependent(type = PipelineDependentResource.class), @Dependent(type = PipelineRunDependentResource.class),
		@Dependent(type = ConfigMapDependentResource.class), })
public class ProjectReconciler implements Reconciler<Project> {
	private final OpenShiftClient client;
	private final TektonClient tektonClient;
	private static final Logger LOG = LoggerFactory.getLogger(ProjectReconciler.class);
	private final EnvironmentServiceImpl envService;
	
	public ProjectReconciler(OpenShiftClient client, EnvironmentServiceImpl envService, TektonClient tektonClient) {
		this.client = client;
		this.envService = envService;
		this.tektonClient = tektonClient;
	}

	@Override
	public UpdateControl<Project> reconcile(Project resource, Context<Project> context) {
		LOG.info("Reconcile");
		UpdateControl<Project> defaultUpdateControl = UpdateControl.noUpdate();
		UpdateControl<Project> defaultUpdateControlSchedule = defaultUpdateControl.rescheduleAfter(Duration.ofSeconds(5));
		
		return getOwningEnvironment(resource).map(e -> {
			Secret userSecret = GiteaUserSecretDependentResource.getResource(e, resource.getSpec().getOwner().getUser(), client).get();
			if (userSecret == null || StringUtil.isNullOrEmpty(userSecret.getData().get("token"))) {
				LOG.info("User secret for {} not present or token not found. Reconciling it.", resource.getSpec().getOwner().getUser());
				GiteaUserSecretDependentResource giteaUserSecretDependentResource = new GiteaUserSecretDependentResource(envService, resource.getSpec().getOwner().getUser(), client);
				giteaUserSecretDependentResource.reconcileDirectly(e, (Context) context);
				return defaultUpdateControlSchedule;
			} else {
				LOG.info("User secret found. Token available. Waiting for pipeline run to complete");
				if (resource.getStatus() == null) {
					resource.setStatus(new ProjectStatus());
				}
				RepositoryStatus repositoryStatus = new RepositoryStatus();
				repositoryStatus.setUserSecretAvailable(true);
				resource.getStatus().setRepository(repositoryStatus);
				if (resource.getStatus().getWorkspace() == null || StringUtil.isNullOrEmpty(resource.getStatus().getWorkspace().getFactoryUrl())) {
					LOG.info("Setting workspace factory url");
					PipelineRunDependentResource.getResource(tektonClient, resource)
					.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCompletionTime()), 10, TimeUnit.MINUTES);
					onPipelineRunComplete(resource, context);
				}
				
				return UpdateControl.patchStatus(resource);
			}
		}).orElseGet(UpdateControl::noUpdate);
	}

	private void onPipelineRunComplete(Project resource, Context<Project> context) {
		WorkspaceStatus status = new WorkspaceStatus();
		Optional<Repository> repository = context.getSecondaryResource(Repository.class);
		repository.ifPresent(r -> {
			String devFilePath = r.getStatus().getInternalCloneUrl().replace(".git", "/raw/branch/main/devfile.yaml");
			status.setFactoryUrl(String.format("%s#%s", getDevSpacesUrl(), devFilePath));
			resource.getStatus().setWorkspace(status);
		});
		
	}

	private Optional<DevEnvironment> getOwningEnvironment(Project owningProject) {
		return Optional.ofNullable(
				client.resources(DevEnvironment.class).inNamespace(owningProject.getSpec().getEnvironmentNamespace())
						.withName(owningProject.getSpec().getEnvironmentName()).get());
	}
	
	private String getDevSpacesUrl() {
		return client.getOpenshiftUrl().getProtocol() + 
				"://" +
				client.getOpenshiftUrl().getHost().replace("api.", "devspaces.apps.");
	}
}
