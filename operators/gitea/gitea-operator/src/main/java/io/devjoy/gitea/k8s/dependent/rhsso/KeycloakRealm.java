package io.devjoy.gitea.k8s.dependent.rhsso;

import org.openapi.quarkus.keycloak_yaml.model.KeycloakRealmSpec;
import org.openapi.quarkus.keycloak_yaml.model.KeycloakRealmStatus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("keycloak.org")
public class KeycloakRealm extends CustomResource<KeycloakRealmSpec, KeycloakRealmStatus> implements Namespaced {

	private static final long serialVersionUID = 2330940188552440333L;

	/*
	 * We need to override otherwise there is a problem calling this for superclass via DependentResource
	 */
	@Override
	public void setSpec(KeycloakRealmSpec spec) {
		super.setSpec(spec);
	}
}
