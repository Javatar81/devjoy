package io.devjoy.operator.repository.k8s;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.repository.service.GitServiceImpl;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Cleaner;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.DeleteControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.quarkus.runtime.util.StringUtil;

public class RepositoryReconciler implements Reconciler<Repository>, Cleaner<Repository> {

	private final KubernetesClient client;
	private GitServiceImpl gitService;
	private static Logger LOG = LoggerFactory.getLogger(RepositoryReconciler.class);

	public RepositoryReconciler(KubernetesClient client, 
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
		if (resource.getSpec().getManaged() != null) {
			return gitService.createIfRepositoryNotExists(resource.getMetadata().getNamespace(), resource).map(r -> {
				LOG.info("Repository {} exists", r.getName());
				resource.getStatus().emitRepositoryExists();
				resource.getStatus().setCloneUrl(r.getCloneUrl());
				return UpdateControl.patchStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
			}).orElseGet(() -> {
				LOG.info("Repository created.");
				resource.getStatus().emitRepositoryCreated();
				resource.getStatus().emitRepositoryExists();
				resource.getStatus().setCloneUrl(resource.getSpec().getExistingRepositoryCloneUrl());
				return UpdateControl.patchResourceAndStatus(resource).rescheduleAfter(10, TimeUnit.SECONDS);
			});
		} else {
			if (StringUtil.isNullOrEmpty(resource.getStatus().getRepositoryExists()) 
					&& !StringUtil.isNullOrEmpty(resource.getSpec().getExistingRepositoryCloneUrl())) {
				LOG.info("Repository not managed, status updated.");
				resource.getStatus().emitRepositoryExists();
				resource.getStatus().setCloneUrl(resource.getSpec().getExistingRepositoryCloneUrl());
				return UpdateControl.patchStatus(resource);
			} else {
				LOG.info("Repository not managed, status ok.");
				return UpdateControl.noUpdate();
			}
		}
	}

	@Override
	public DeleteControl cleanup(Repository resource, Context<Repository> context) {
		if(resource.getStatus() == null || StringUtil.isNullOrEmpty(resource.getStatus().getRepositoryExists())) {
			LOG.info("Repository has been created before creating this resource. Skipping deletion");
		} else if (resource.getStatus() != null 
				&& resource.getSpec().getManaged() != null 
				&& resource.getSpec().getManaged().isDeleteRepoOnFinalize()){
			LOG.info("Deleting repository");
			gitService.delete(resource);
		}
		return DeleteControl.defaultDelete();
	}

	

}
