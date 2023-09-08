package io.devjoy.gitea.domain;

import java.io.ByteArrayOutputStream;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.gitea.GiteaDeploymentDependentResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GiteaPodExecService {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaPodExecService.class);
	private final KubernetesClient client;
	
	public GiteaPodExecService(KubernetesClient client) {
		super();
		this.client = client;
	}


	public Optional<String> execOnDeployment(Gitea gitea, Command cmd) {
		LOG.debug("Executing {}", cmd);
		Optional<Deployment> deployment = Optional.ofNullable(GiteaDeploymentDependentResource.getResource(gitea, client).waitUntilCondition(c -> c.getStatus().getReadyReplicas() != null && c.getStatus().getReadyReplicas() > 0, 180, TimeUnit.SECONDS));
		return deployment.flatMap(d -> {
			Optional<ReplicaSet> replicaSet = client.apps().replicaSets()
				.inNamespace(gitea.getMetadata().getNamespace())
				.list()
				.getItems()
				.stream()
				.filter(r -> r.getOwnerReferenceFor(d.getMetadata().getUid()).isPresent())
				.max(Comparator.comparingInt(r -> Integer.valueOf(r.getMetadata().getAnnotations().get("deployment.kubernetes.io/revision"))));
			return replicaSet.flatMap(rs -> execOnReplicaSetPod(gitea, rs, cmd));
		});
	}
	
	
	public Optional<String> execOnReplicaSetPod(Gitea gitea, ReplicaSet rs, Command cmd) {
		return client.pods()
			.inNamespace(gitea.getMetadata().getNamespace())
			.list()
			.getItems()
			.stream()
			.filter(p -> p.getOwnerReferenceFor(rs.getMetadata().getUid()).isPresent())
			.findAny()
			.map(p -> this.exec(p, cmd));
	}

	public String exec(Pod p, Command cmd) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream error = new ByteArrayOutputStream();
		CountDownLatch execLatch = new CountDownLatch(1);
		try (ExecWatch execWatch = client.pods().inNamespace(p.getMetadata().getNamespace()).withName(p.getMetadata().getName())
				.writingOutput(out)
		        .writingError(error)
		        .usingListener(new ExecListener() {
	                @Override
	                public void onFailure(Throwable throwable, Response response) {
	                    execLatch.countDown();
	                }

	                @Override
	                public void onClose(int i, String s) {
	                    execLatch.countDown();
	                }
	            })
		        .exec(cmd.toArray())) {   
			boolean latchTerminationStatus = execLatch.await(15, TimeUnit.SECONDS);
			if (!latchTerminationStatus) {
			  throw new ServiceException("Timeout while waiting for command to complete");
			}
			LOG.debug("Exec Output: {} ", out);
			if (error.size() > 0) {
				LOG.error("Err Output: {} ", error);
			}
			if (error.toString().length() > 0) {
				throw new ServiceException(error.toString());
			}
			return out.toString();
		} catch (InterruptedException ie) {
			Thread.currentThread().interrupt();
			throw new ServiceException("Interrupted while waiting for the exec", ie);
	    } 
	}
}
