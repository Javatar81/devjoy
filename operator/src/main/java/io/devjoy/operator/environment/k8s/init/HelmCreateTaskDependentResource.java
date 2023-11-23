package io.devjoy.operator.environment.k8s.init;

import java.util.HashMap;
import java.util.Optional;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.pipeline.v1beta1.Task;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent(resourceDiscriminator = HelmCreateTaskDiscriminator.class,labelSelector = HelmCreateTaskDependentResource.LABEL_SELECTOR)
public class HelmCreateTaskDependentResource extends CRUDKubernetesDependentResource<Task, DevEnvironment>{
	private static final String LABEL_KEY = "devjoy.io/task.type";
	private static final String LABEL_VALUE = "helm-create";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@Inject
	TektonClient tektonClient;
	
	public HelmCreateTaskDependentResource() {
		super(Task.class);
	}
	
	@Override
	public Task create(Task target, DevEnvironment primary, Context<DevEnvironment> context) {
		Optional<Task> existingTask = Optional.ofNullable(tektonClient.v1beta1()
				.tasks().withName(getName()).get());
		return existingTask.orElseGet(() -> super.create(target, primary, context));
	}

    public static String getName() {
        return "helm-create";
    }
	
	@Override
	protected Task desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Task task = tektonClient.v1beta1()
				.tasks()
				.load(getClass().getClassLoader().getResourceAsStream("deploy/helm-create-task.yaml"))
				.item();
		task.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (task.getMetadata().getLabels() == null) {
			task.getMetadata().setLabels(new HashMap<>());
		}
		task.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return task;
	}
}

