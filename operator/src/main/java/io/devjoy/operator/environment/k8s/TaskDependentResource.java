package io.devjoy.operator.environment.k8s;

import javax.inject.Inject;

import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class TaskDependentResource extends CRUDKubernetesDependentResource<Task, DevEnvironment>{
	@Inject
	TektonClient tektonClient;
	
	public TaskDependentResource() {
		super(Task.class);
	}
	
	@Override
	protected Task desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Task task = tektonClient.v1beta1()
				.tasks()
				.load(getClass().getClassLoader().getResourceAsStream("init/quarkus-create-task.yaml"))
				.get();
		task.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return task;
	}
}
