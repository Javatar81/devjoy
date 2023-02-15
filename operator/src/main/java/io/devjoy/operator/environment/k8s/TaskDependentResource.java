package io.devjoy.operator.environment.k8s;

import java.util.Optional;

import javax.inject.Inject;

import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.ClusterTask;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class TaskDependentResource extends CRUDKubernetesDependentResource<ClusterTask, DevEnvironment>{
	@Inject
	TektonClient tektonClient;
	
	public TaskDependentResource() {
		super(ClusterTask.class);
	}
	
	@Override
	public ClusterTask create(ClusterTask target, DevEnvironment primary, Context<DevEnvironment> context) {
		Optional<ClusterTask> existingTask = Optional.ofNullable(tektonClient.v1beta1()
				.clusterTasks().withName("quarkus-create").get());
		System.out.println("TTTTTTTTTTTTTTTTT " + existingTask);
		return existingTask.orElseGet(() -> super.create(target, primary, context));
	}
	
	@Override
	protected ClusterTask desired(DevEnvironment primary, Context<DevEnvironment> context) {
		ClusterTask task = tektonClient.v1beta1()
				.clusterTasks()
				.load(getClass().getClassLoader().getResourceAsStream("init/quarkus-create-task.yaml"))
				.get();
		return task;
	}
}
