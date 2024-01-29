package io.devjoy.gitea.k8s.dependent.rhsso;

import org.openapi.quarkus.keycloak_yaml.model.KeycloakSpec;
import org.openapi.quarkus.keycloak_yaml.model.KeycloakStatus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("keycloak.org")
public class Keycloak  extends CustomResource<KeycloakSpec, KeycloakStatus> implements Namespaced {
	
	private static final long serialVersionUID = 6961678327359140631L;

	/*
	 * We need to override otherwise there is a problem calling this for superclass via DependentResource
	 */
	@Override
	public void setSpec(KeycloakSpec spec) {
		super.setSpec(spec);
	}
	
}
