package io.devjoy.operator.environment.k8s.init;

import java.util.HashMap;
import java.util.Optional;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1.Task;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent(resourceDiscriminator = QuarkusTaskDiscriminator.class,labelSelector = QuarkusTaskDependent.LABEL_SELECTOR)
public class QuarkusTaskDependent extends CRUDKubernetesDependentResource<Task, DevEnvironment>{
	private static final String LABEL_KEY = "devjoy.io/task.type";
	private static final String LABEL_VALUE = "quarkus";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@Inject
	TektonClient tektonClient;
	
	public QuarkusTaskDependent() {
		super(Task.class);
	}
	
	@Override
	public Task create(Task target, DevEnvironment primary, Context<DevEnvironment> context) {
		Optional<Task> existingTask = Optional.ofNullable(tektonClient.v1()
				.tasks().withName("quarkus-create").get());
		return existingTask.orElseGet(() -> super.create(target, primary, context));
	}
	
	@Override
	protected Task desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Task task = tektonClient.v1()
				.tasks()
				.load(getClass().getClassLoader().getResourceAsStream("init/quarkus-create-task.yaml"))
				.item();
		task.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (task.getMetadata().getLabels() == null) {
			task.getMetadata().setLabels(new HashMap<>());
		}
		task.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return task;
	}
}
