package io.devjoy.gitea.k8s.dependent.rhsso;

import org.openapi.quarkus.keycloak_yaml.model.KeycloakClientSpec;
import org.openapi.quarkus.keycloak_yaml.model.KeycloakClientStatus;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("keycloak.org")
public class KeycloakClient extends CustomResource<KeycloakClientSpec, KeycloakClientStatus> implements Namespaced {

	private static final long serialVersionUID = -3631227576642750007L;

	/*
	 * We need to override otherwise there is a problem calling this for superclass via DependentResource
	 */
	@Override
	public void setSpec(KeycloakClientSpec spec) {
		super.setSpec(spec);
	}
	
	public static Resource<KeycloakClient> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(KeycloakClient.class).inNamespace(primary.getMetadata().getNamespace())
				.withName(resourceName(primary));
	}
	
	public static String resourceName(Gitea primary) {
		return primary.getMetadata().getName() + "-devjoy";
	}

}
