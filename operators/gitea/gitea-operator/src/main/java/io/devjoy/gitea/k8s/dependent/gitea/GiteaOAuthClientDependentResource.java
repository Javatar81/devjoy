package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.OAuthClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;
import io.quarkus.runtime.util.StringUtil;
import jakarta.inject.Inject;

@KubernetesDependent(labelSelector = GiteaOAuthClientDependentResource.LABEL_SELECTOR)
public class GiteaOAuthClientDependentResource extends KubernetesDependentResource<OAuthClient, Gitea> implements Creator<OAuthClient, Gitea>, Updater<OAuthClient, Gitea>, Matcher<OAuthClient, Gitea> {
	private static final String OAUTH2_CALLBACK = "/user/oauth2/test/callback";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaOAuthClientDependentResource.class);
	private static final String LABEL_KEY = "devjoy.io/oauthclient.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@Inject
	GiteaApiService apiService;
	@Inject
	OpenShiftClient ocpClient;
	@Inject
	PasswordService passwordService;
	
	public GiteaOAuthClientDependentResource() {
		super(OAuthClient.class);
	}

	

	/*
	 * We override this to get resource via client because it seems not to be cached.
	 * This might be caused by namespaceless nature of the OAuthClient resource.
	 */
	@Override
	public Optional<OAuthClient> getSecondaryResource(Gitea primaryResource, Context<Gitea> ctx) {
		return super.getSecondaryResource(primaryResource, ctx).or(() -> Optional.ofNullable(getResource(primaryResource, ocpClient).get()));
	}
	
	
	@Override
	protected OAuthClient desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired state");
		OAuthClient client = ocpClient.resources(OAuthClient.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/oauth-client.yaml")).item();
		String name = clientName(primary);
		client.getMetadata().setName(name);
		ArrayList<String> redirectURIs = new ArrayList<>();
		client.setRedirectURIs(redirectURIs);
		String callbackPath = OAUTH2_CALLBACK;
		apiService.getRouterBaseUri(primary).ifPresent(uri -> redirectURIs.add(uri + callbackPath));
		redirectURIs.add(apiService.getLocalBaseUri(primary) + callbackPath);
		if (primary.getSpec() != null && primary.getSpec().isSso()) {
			Optional<String> keycloakUrl = Optional.ofNullable(KeycloakDependentResource.getResource(primary, ocpClient)
					.waitUntilCondition(c -> c!= null && c.getStatus() != null && !StringUtil.isNullOrEmpty(c.getStatus().getExternalURL()), 180, TimeUnit.SECONDS))
					.map(k -> k.getStatus().getExternalURL());
			keycloakUrl.ifPresent(url -> 
				redirectURIs.add(String.format("%s/auth/realms/%s/broker/%s/endpoint", url, KeycloakRealmDependentResource.resourceName(primary), KeycloakRealmDependentResource.alias(primary)))	
			);
		}
		Optional<OAuthClient> existingClient = Optional.ofNullable(GiteaOAuthClientDependentResource.getResource(primary, ocpClient).get());
		existingClient.ifPresentOrElse(c -> {
			if (StringUtil.isNullOrEmpty(c.getSecret())) {
				client.setSecret(passwordService.generateNewPassword(12));
			} else {
				client.setSecret(c.getSecret());
			}
		}, () -> 
			client.setSecret(passwordService.generateNewPassword(12))
		);
		if (client.getMetadata().getLabels() == null) {
			client.getMetadata().setLabels(new HashMap<>());
		}
		client.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return client;
	}
	
	@Override
	public Result<OAuthClient> match(OAuthClient actualResource, Gitea primary,
		      Context<Gitea> context) {
		var desired = this.desired(primary, context);
		boolean equal = Objects.equals(actualResource.getMetadata().getName(), desired.getMetadata().getName())
		 && Objects.equals(actualResource.getMetadata().getNamespace(), desired.getMetadata().getNamespace())
		 && Objects.equals(actualResource.getSecret(), desired.getSecret())
		 && Objects.equals(actualResource.getRedirectURIs(), desired.getRedirectURIs())
		 && Objects.equals(actualResource.getGrantMethod(), desired.getGrantMethod());
	    return Result.computed(equal, desired);
	}

	public static String clientName(Gitea primary) {
		return primary.getMetadata().getName() + "-client";
	}
	
	public static Resource<OAuthClient> getResource(Gitea primary, OpenShiftClient client) {
		return client.oAuthClients().withName(clientName(primary));
	}
	
}
