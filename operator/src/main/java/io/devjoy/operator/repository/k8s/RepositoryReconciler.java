package io.devjoy.operator.repository.k8s;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.project.k8s.Project;
import io.devjoy.operator.repository.service.GitServiceImpl;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.runtime.util.StringUtil;

public class RepositoryReconciler implements Reconciler<Repository>, Cleaner<Repository> {

	private final KubernetesClient client;
	private GitServiceImpl gitService;
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryReconciler.class);

	public RepositoryReconciler(OpenShiftClient client, 
			GitServiceImpl gitService) {
		this.client = client;
		this.gitService = gitService;
	}

	@Override
	public UpdateControl<Repository> reconcile(Repository resource, Context<Repository> context) {
		LOG.info("Reconciling {} ", resource);
		if (resource.getStatus() == null) {
			resource.setStatus(new RepositoryStatus());
		}
		Optional<DevEnvironment> env = getDevEnvironment(resource);
		return env.map( 
			e -> syncRepository(resource, e)
		).orElseGet(UpdateControl::noUpdate);

		
	}

	private Optional<DevEnvironment> getDevEnvironment(Repository resource) {
		Optional<OwnerReference> ownerRef = resource.getMetadata().getOwnerReferences().stream()
				.filter(o -> "Project".equalsIgnoreCase(o.getKind()))
				.findFirst();
		Optional<Project> owningProject = getOwningProject(resource, ownerRef);
		return getOwningEnvironment(owningProject);
	}

	private UpdateControl<Repository> syncRepository(Repository resource, DevEnvironment env) {
		return getAdminUser(env).map(adminUser -> {
			if (resource.getSpec().getManaged() != null) {
				assureRepoUserExists(resource, adminUser, env);
				return handleManagedRepository(resource, adminUser, env);
			} else {
				return handleUnmanagedRepository(resource);
			}
		}).orElseGet(() -> {
			LOG.info("Admin user not available");
			UpdateControl<Repository> noUpdate = UpdateControl.noUpdate();
			return noUpdate;	
		});
	}

	private UpdateControl<Repository> handleUnmanagedRepository(Repository resource) {
		if (StringUtil.isNullOrEmpty(resource.getStatus().getRepositoryExists()) 
				&& !StringUtil.isNullOrEmpty(resource.getSpec().getExistingRepositoryCloneUrl())) {
			LOG.info("Repository not managed, status updated.");
			resource.getStatus().emitRepositoryExists();
			resource.getStatus().setCloneUrl(resource.getSpec().getExistingRepositoryCloneUrl());
			UpdateControl<Repository> update =  UpdateControl.patchStatus(resource);
			return update;
		} else {
			LOG.info("Repository not managed, status ok.");
			UpdateControl<Repository> noUpdate = UpdateControl.noUpdate();
			return noUpdate;
		}
	}

	private void assureRepoUserExists(Repository resource, String adminUser, DevEnvironment env) {
		boolean userExist = gitService.repoUserExists(resource.getSpec().getManaged().getUser(), resource.getMetadata().getNamespace(), resource, adminUser, env);
		if (!userExist) {
			LOG.info("Repository user does not exist, creating {}.", resource.getSpec().getManaged().getUser());
			gitService.createRepoUser(resource.getSpec().getManaged().getUser(), resource.getSpec().getManaged().getUser() + "@example.com", resource.getMetadata().getNamespace(), resource, adminUser, env);
		}
	}

	private UpdateControl<Repository> handleManagedRepository(Repository resource, String adminUser, DevEnvironment env) {
		return gitService.createIfRepositoryNotExists(resource.getMetadata().getNamespace(), resource, adminUser, env).map(r -> {
			LOG.info("Repository {} exists", r.getName());
			resource.getStatus().emitRepositoryExists();
			resource.getStatus().setCloneUrl(r.getCloneUrl());
			determineInternalCloneUrl(r.getCloneUrl(), env)
					.ifPresent(url -> resource.getStatus().setInternalCloneUrl(url));
			return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
		}).orElseGet(() -> {
			LOG.info("Repository created.");
			resource.getStatus().emitRepositoryCreated();
			resource.getStatus().emitRepositoryExists();
			resource.getStatus().setCloneUrl(resource.getSpec().getExistingRepositoryCloneUrl());
			determineInternalCloneUrl(resource.getSpec().getExistingRepositoryCloneUrl(), env)
				.ifPresent(url -> resource.getStatus().setInternalCloneUrl(url));
			return UpdateControl.patchResourceAndStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
		});
	}

	private Optional<String> getAdminUser(DevEnvironment env) {
		return Optional.ofNullable(GiteaDependentResource.getResource(client, env)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getSpec().getGiteaAdminUser()), 30, TimeUnit.SECONDS)
				.getSpec().getGiteaAdminUser());
	}

	private Optional<DevEnvironment> getOwningEnvironment(Optional<Project> owningProject) {
		return owningProject.map(p -> 
			client.resources(DevEnvironment.class).inNamespace(p.getSpec().getEnvironmentNamespace()).withName(p.getSpec().getEnvironmentName()).get()
		).or(() -> {
			LOG.warn("Owning project not found");
			return Optional.empty();
		});
	}

	private Optional<Project> getOwningProject(Repository resource, Optional<OwnerReference> ownerRef) {
		return ownerRef.map(r -> 
		client.resources(Project.class).inNamespace(resource.getMetadata().getNamespace()).withName(r.getName()).get()
		).or(() -> {
			LOG.warn("Owner reference not found");
			return Optional.empty();
		});
	}

	@Override
	public DeleteControl cleanup(Repository resource, Context<Repository> context) {
		if(resource.getStatus() == null || StringUtil.isNullOrEmpty(resource.getStatus().getRepositoryExists())) {
			LOG.info("Repository has been created before creating this resource. Skipping deletion");
		} else if (resource.getStatus() != null 
				&& resource.getSpec().getManaged() != null 
				&& resource.getSpec().getManaged().isDeleteRepoOnFinalize()){
			LOG.info("Deleting repository");
			Optional<DevEnvironment> env = getDevEnvironment(resource);
			if (env.isPresent()) {
				getAdminUser(env.get()).ifPresent(adminUser -> gitService.delete(resource, adminUser, env.get()));
			} else {
				LOG.warn("Skipping delete. Environment not available.");
			}
			
		}
		return DeleteControl.defaultDelete();
	}

	private Optional<String> determineInternalCloneUrl(String externalCloneUrl, DevEnvironment devEnv) {
		try {
			URL url = new URL(externalCloneUrl);
			Optional<Service> giteaService = client.services().inNamespace(devEnv.getMetadata().getNamespace())
					.withLabel("app", GiteaDependentResource.getResource(client, devEnv).get().getMetadata().getName())
					.list().getItems().stream()
					.findAny();
				return giteaService
						.map(s -> String.format("http://%s.%s.svc.cluster.local:%d%s", s.getMetadata().getName(), 
								s.getMetadata().getNamespace(),
								s.getSpec().getPorts()
								.stream().filter(p -> "gitea".equals(p.getName()))
								.map(ServicePort::getPort)
								.findAny()
								.orElse(80), url.getPath()));
		} catch (MalformedURLException e) {
			LOG.warn("No valid external clone url", e);
			return Optional.empty();
		}
	}
}
