package io.devjoy.gitea.repository.k8s;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.gitea.GiteaRouteDependentResource;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Base64;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteSpec;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.quarkus.runtime.util.StringUtil;

public class GiteaUserSecretDependentResource extends CRUDKubernetesDependentResource<Secret, Gitea> {
	private static final String LABEL_KEY = "devjoy.io/secret.type";
	private static final String LABEL_VALUE = "user";
	private static final String TOKEN_KEY = "token";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaUserSecretDependentResource.class);
	private String username;
	private TokenService tokenService;
	
	public GiteaUserSecretDependentResource() {
		super(Secret.class);
	}
	
	public GiteaUserSecretDependentResource(
			String username, OpenShiftClient client, TokenService tokenService) {
		super(Secret.class);
		this.username = username;
		super.client = client;
		this.tokenService = tokenService;
	}

	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired state");
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/user-secret.yaml")).get();
		desired.getMetadata().setName(username + desired.getMetadata().getName());
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put("user", Base64.encodeBytes(
				username.getBytes()));
		Route routeFromGitea = getRouteFromGitea(primary);
		String giteaRouteWithProtocol = String.format("%s://%s", "http" + (routeFromGitea.getSpec().getTls() != null ? "s": ""), routeFromGitea.getSpec().getHost());
		
		desired.getData().put(".gitconfig", Base64.encodeBytes(
				String.format("[credential \"%s\"]\n"
						+ "\nhelper = store", giteaRouteWithProtocol).getBytes()));
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		desired.getMetadata().setLabels(labels);
		addOwnerReference(primary, desired);
		//Replace the token because reconcile will call this again and we can't get the token anymore
		Secret existingSecret = getResource(primary, username, client).get();
		if (existingSecret != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(TOKEN_KEY))) {
			LOG.info("Token already set. Taking it over from existing to desired.");
			desired.getData().put(TOKEN_KEY, existingSecret.getData().get(TOKEN_KEY));
		} else {
			LOG.info("Token not set. Generating new token.");
			tokenService.replaceUserTokenViaCli(primary, username, "devjoy")
			.ifPresentOrElse(t -> {
				LOG.info("Updating token for secret {}", desired.getMetadata().getName());
				desired.getData().put(TOKEN_KEY, Base64.encodeBytes(t.getBytes()));
				getGitCredentials(username, t, giteaRouteWithProtocol).ifPresent(c ->
				desired.getData().put(".git-credentials", Base64.encodeBytes(
						c.getBytes())));
			}, () -> LOG.warn("Cannot update token."));
		}
		return desired;
	}

	private Route getRouteFromGitea(Gitea primary) {
		return GiteaRouteDependentResource.getResource(primary, (OpenShiftClient) client)
				.waitUntilCondition(c -> c != null &&!StringUtil.isNullOrEmpty(c.getSpec().getHost()), 10, TimeUnit.SECONDS);
	}
	
	

	private void addOwnerReference(Gitea primary, Secret desired) {
		List<OwnerReference> ownerReferences = new ArrayList<>();
		ownerReferences.add(new OwnerReference(primary.getApiVersion(), true, true, primary.getKind(), primary.getMetadata().getName(), primary.getMetadata().getUid()));
		desired.getMetadata().setOwnerReferences(ownerReferences);
	}
	
	public void reconcileDirectly(Gitea primary, Context<Gitea> context) {
		Secret desired = desired(primary, context);
		client.secrets().inNamespace(desired.getMetadata().getNamespace()).createOrReplace(desired);
	}
	
	private Optional<String> getGitCredentials(String username, String token, String gitBaseUrl) {
		try {
			URL url = new URL(gitBaseUrl);
			return Optional.of(String.format("%s://%s:%s@%s%s", url.getProtocol(), username, token, url.getHost(), url.getFile()));
		} catch (MalformedURLException e) {
			LOG.error("Invalid gitBaseUrl " + gitBaseUrl, e);
			return Optional.empty();
		}
		
	}

	public static Resource<Secret> getResource(Gitea primary, String username, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				username + "-git-secret");
	}
}
