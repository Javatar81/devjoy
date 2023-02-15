package io.devjoy.operator.environment.k8s.build;

import java.util.HashMap;
import java.util.Optional;

import javax.inject.Inject;

import io.devjoy.gitea.domain.PasswordService;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Base64;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = WebhookSecretDependentResource.LABEL_TYPE_SELECTOR)
public class WebhookSecretDependentResource extends CRUDKubernetesDependentResource<Secret, DevEnvironment>{
	private static final String KEY_WEBHOOK_SECRET = "webhook-secret";
	private static final String LABEL_TYPE_KEY = "devjoy.io/secret.type";
	private static final String LABEL_TYPE_VALUE = "webhook";
	static final String LABEL_TYPE_SELECTOR = LABEL_TYPE_KEY + "=" + LABEL_TYPE_VALUE;
	
	@Inject
	PasswordService passwordService;
	
	public WebhookSecretDependentResource() {
		super(Secret.class);
	}
	
	@Override
	protected Secret desired(DevEnvironment primary, Context<DevEnvironment> context) {
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("build/webhook-secret.yaml")).get();
		desired.getMetadata().setName(getName(primary));
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_TYPE_KEY, LABEL_TYPE_VALUE);
		desired.getMetadata().setLabels(labels);
		Optional.ofNullable(getResource(primary, client).get())
			.map(s -> s.getData().get(KEY_WEBHOOK_SECRET))
			.ifPresentOrElse(pw -> desired.getData().put(KEY_WEBHOOK_SECRET, pw),
				() -> 
					desired.getData().put(KEY_WEBHOOK_SECRET, Base64.encodeBytes(
							passwordService.generateNewPassword(12).getBytes()))
			);
		return desired;
	}
	
	public static String getName(DevEnvironment primary) {
		return "webhook-secret-" + primary.getMetadata().getName();
	}
	
	public static Resource<Secret> getResource(DevEnvironment primary, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				getName(primary));
	}
}
