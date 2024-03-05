package io.devjoy.gitea.k8s.dependent.rhsso;

import static io.netty.util.internal.StringUtil.isNullOrEmpty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

import org.keycloak.v1alpha1.KeycloakClient;
import org.keycloak.v1alpha1.KeycloakClientSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.service.AuthenticationService;
import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public class KeycloakClientDependentResource extends CRUDKubernetesDependentResource<KeycloakClient, Gitea> {
	private static final int SECRET_LENGTH = 12;
	private static final String LABEL_DEVJOY_APP = "app.devjoy.io/gitea";
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakClientDependentResource.class);
	@Inject
	PasswordService passwordService;
	@Inject
	GiteaApiService giteaApiService;
	@Inject
	AuthenticationService authenticationService;
	
	public KeycloakClientDependentResource() {
		super(KeycloakClient.class);
	}
	
	@Override
	protected KeycloakClient desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Reconciling");
		KeycloakClient keycloakClient = context.getClient().resources(KeycloakClient.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/rhsso/client.yaml")).item();
		ObjectMeta metadata = keycloakClient.getMetadata();
		KeycloakClientSpec spec = keycloakClient.getSpec();
		metadata.setNamespace(primary.getMetadata().getNamespace());
		metadata.setName(resourceName(primary));
		if (metadata.getLabels() == null) {
			metadata.setLabels(new HashMap<>());
		}
		if (spec.getRealmSelector().getMatchLabels() == null) {
			spec.getRealmSelector().setMatchLabels(new HashMap<>());
		}
		spec.getRealmSelector().getMatchLabels().put(LABEL_DEVJOY_APP, primary.getMetadata().getName());
		spec.getClient().setClientId(clientId(primary));
		if (spec.getClient().getRedirectUris() == null) {
			spec.getClient().setRedirectUris(new ArrayList<>());
		}
		
		giteaApiService.getRouterBaseUri(primary).ifPresent(baseUri -> {
			String routerRedirectUri = String.format("%s/user/oauth2/%s/callback", baseUri, "devjoy-oidc");
			spec.getClient().getRedirectUris().add(routerRedirectUri);
		});
		String internalRedirectUri = String.format("%s/user/oauth2/%s/callback", giteaApiService.getLocalBaseUri(primary), "devjoy-oidc");
		spec.getClient().getRedirectUris().add(internalRedirectUri);
		Optional.ofNullable(getResource(primary, context.getClient()).get())
			.map(c -> c.getSpec().getClient())
			.ifPresentOrElse(c -> {
				if(isNullOrEmpty(c.getSecret())) {
					LOG.info("Setting secret for existing client instance because it was empty");
					spec.getClient().setSecret(passwordService.generateNewPassword(SECRET_LENGTH));
				} else {
					spec.getClient().setSecret(c.getSecret());
				}
			}, () -> {
				LOG.info("Setting secret for new client instance");
				spec.getClient().setSecret(passwordService.generateNewPassword(SECRET_LENGTH));
			});
		return keycloakClient;
	}

	public static String resourceName(Gitea primary) {
		return primary.getMetadata().getName() + "-devjoy";
	}
	
	public static String clientId(Gitea primary) {
		return  primary.getMetadata().getName() + "-devjoy-gitea";
	}
	
	public static Resource<KeycloakClient> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(KeycloakClient.class).inNamespace(primary.getMetadata().getNamespace())
				.withName(resourceName(primary));
	}

}
