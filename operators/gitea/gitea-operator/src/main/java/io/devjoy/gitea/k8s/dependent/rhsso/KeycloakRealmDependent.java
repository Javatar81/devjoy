package io.devjoy.gitea.k8s.dependent.rhsso;

import java.util.HashMap;
import java.util.Optional;

import org.keycloak.v1alpha1.KeycloakRealm;
import org.keycloak.v1alpha1.KeycloakRealmSpec;
import org.keycloak.v1alpha1.keycloakrealmspec.realm.IdentityProviders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent
public class KeycloakRealmDependent extends CRUDKubernetesDependentResource<KeycloakRealm, Gitea> {
	private static final String LABEL_DEVJOY_APP = "app.devjoy.io/gitea";
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakRealmDependent.class);
	@Inject
	OpenShiftClient ocpClient;
	@Inject
	PasswordService passwordService;
	public KeycloakRealmDependent() {
		super(KeycloakRealm.class);
		
	}

	@Override
	protected KeycloakRealm desired(Gitea primary, Context<Gitea> context) {
		KeycloakRealm realm = context.getClient().resources(KeycloakRealm.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/rhsso/realm.yaml")).item();
		realm.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		String name = resourceName(primary);
		realm.getMetadata().setName(name);
		KeycloakRealmSpec spec = realm.getSpec();
		Optional<IdentityProviders> ocpProvider = getDevjoyIdentityProvider(spec);
		spec.getRealm().setRealm(name);
		spec.getRealm().setId(name);
		spec.getRealm().setDisplayName(primary.getMetadata().getName());
	
		Optional<OAuthClient> oauthClient = context.getSecondaryResource(OAuthClient.class);
		LOG.info("OauthClient present? {}", oauthClient.isPresent());
		
		oauthClient.ifPresent(oauth -> 
			ocpProvider
				.filter(p -> alias(primary).equals(p.getAlias()))
				.map(IdentityProviders::getConfig)
				.ifPresent(c -> {
					c.put("clientSecret", oauth.getSecret());
					c.put("baseUrl", ocpClient.getMasterUrl().toString());
					c.put("clientId", primary.getMetadata().getName() + "-client");
			})
		);
		
		
		/*
		Optional.ofNullable(getResource(primary, client).get())
			.ifPresentOrElse(c -> 
				ocpProvider
					.map(KeycloakRealmSpecRealmIdentityProviders::getConfig)
					.ifPresent(cfg -> {
						if(isNullOrEmpty(getDevjoyIdentityProvider(c.getSpec()).map(i -> i.getConfig().get("clientSecret")).orElse(null))) {
							LOG.info("Setting secret for existing realm instance because it was empty");
							cfg.put("clientSecret", passwordService.generateNewPassword(SECRET_LENGTH));
						}
					}), 
				() -> {
					LOG.info("Setting secret for new realm instance");
					ocpProvider.map(KeycloakRealmSpecRealmIdentityProviders::getConfig)
					.ifPresent(cfg -> cfg.put("clientSecret", passwordService.generateNewPassword(SECRET_LENGTH)));
				}); */
											
		if (realm.getMetadata().getLabels() == null) {
			realm.getMetadata().setLabels(new HashMap<>());
		}
		realm.getMetadata().getLabels().put(LABEL_DEVJOY_APP, primary.getMetadata().getName());
		
		return realm;
	}

	private Optional<IdentityProviders> getDevjoyIdentityProvider(KeycloakRealmSpec spec) {
		return spec.getRealm().getIdentityProviders().stream()
			.filter(i -> "devjoy-ocp".equals(i.getAlias()))
			.findAny();
	}
	
	public static String resourceName(Gitea primary) {
		return primary.getMetadata().getName() + "-devjoy";
	}
	
	public static String alias(Gitea primary) {
		return "devjoy-ocp";
	}
	
	
	
	public static Resource<KeycloakRealm> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(KeycloakRealm.class).inNamespace(primary.getMetadata().getNamespace())
				.withName(resourceName(primary));
	}

}
