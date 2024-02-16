package io.devjoy.gitea.k8s.dependent.rhsso;


import java.util.HashMap;

import org.keycloak.v1alpha1.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class KeycloakDependentResource extends CRUDKubernetesDependentResource<Keycloak, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakDependentResource.class);
	public KeycloakDependentResource() {
		super(Keycloak.class);
	}
	
	@Override
	protected Keycloak desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired state from gitea {}", primary.getMetadata().getName());
		Keycloak keycloak = context.getClient().resources(Keycloak.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/rhsso/keycloak.yaml")).item();
		keycloak.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		String name = resourceName(primary);
		keycloak.getMetadata().setName(name);
		if (keycloak.getMetadata().getLabels() == null) {
			keycloak.getMetadata().setLabels(new HashMap<>());
		}
		keycloak.getMetadata().getLabels().put("app.kubernetes.io/part-of", primary.getMetadata().getName()); 

		return keycloak;
	}
	
	public static Resource<Keycloak> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(Keycloak.class).inNamespace(primary.getMetadata().getNamespace())
				.withName(resourceName(primary));
	}
	
	private static String resourceName(Gitea primary) {
		return primary.getMetadata().getName() + "-devjoy";
	}

	
}
