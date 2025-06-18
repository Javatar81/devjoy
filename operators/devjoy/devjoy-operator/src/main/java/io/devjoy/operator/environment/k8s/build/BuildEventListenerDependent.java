package io.devjoy.operator.environment.k8s.build;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.DevEnvironmentReconciler;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.tekton.client.TektonClient;
import io.fabric8.tekton.triggers.v1beta1.EventListener;
import io.fabric8.tekton.triggers.v1beta1.EventListenerTrigger;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public class BuildEventListenerDependent extends CRUDKubernetesDependentResource<EventListener, DevEnvironment>{
	private static final Logger LOG = LoggerFactory.getLogger(DevEnvironmentReconciler.class);
	@Inject
	TektonClient tektonClient;
	
	public BuildEventListenerDependent() {
		super(EventListener.class);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected EventListener desired(DevEnvironment primary, Context<DevEnvironment> context) {
		LOG.debug("Reconciling desired state.");
		EventListener eventListener = tektonClient.v1beta1()
				.eventListeners()
				.load(getClass().getClassLoader().getResourceAsStream("build/build-event-listener.yaml"))
				.item();
		eventListener.getMetadata().setName(getName(primary));
		eventListener.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		Optional<EventListenerTrigger> trigger = eventListener.getSpec().getTriggers().stream().findFirst();
		trigger.ifPresent(t -> {
			t.getBindings().stream().findFirst()
				.ifPresent(b -> b.setRef(b.getRef() + primary.getMetadata().getName()));
			t.getInterceptors().stream().findFirst()
				.ifPresent(i -> i.getParams().stream()
						.filter(p -> "secretRef".equals(p.getName()))
						.findFirst()
							.map(sref -> (ObjectNode) sref.getValue())
							.ifPresent(sref -> sref.put("secretName", WebhookSecretDependent.getName(primary))));
			t.getTemplate().setRef(BuildPushTriggerTemplateDependent.getName(primary));
		});
		return eventListener;
	}
	
	private static String getName(DevEnvironment primary) {
		return "git-new-push-" + primary.getMetadata().getName();
	}
	
	public static Resource<EventListener> getResource(DevEnvironment primary, KubernetesClient client) {
		return client.resources(EventListener.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				getName(primary));
	}

}
