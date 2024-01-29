package io.devjoy.operator.environment.k8s.build;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.triggers.v1beta1.TriggerBinding;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public class GiteaPushTriggerBindingDependentResource extends CRUDKubernetesDependentResource<TriggerBinding, DevEnvironment>{
	@Inject
	TektonClient tektonClient;
	
	public GiteaPushTriggerBindingDependentResource() {
		super(TriggerBinding.class);
	}
	
	@Override
	protected TriggerBinding desired(DevEnvironment primary, Context<DevEnvironment> context) {
		TriggerBinding triggerBinding = tektonClient.v1beta1()
				.triggerBindings()
				.load(getClass().getClassLoader().getResourceAsStream("build/gitea-trigger-binding.yaml"))
				.item();
		triggerBinding.getMetadata().setName(getName(primary, triggerBinding));
		triggerBinding.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return triggerBinding;
	}

	private static String getName(DevEnvironment primary, TriggerBinding triggerBinding) {
		return triggerBinding.getMetadata().getName() + primary.getMetadata().getName();
	}
}
