package io.devjoy.operator.environment.k8s;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.service.EnvironmentServiceImpl;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Base64;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.quarkus.runtime.util.StringUtil;

public class GiteaUserSecretDependentResource extends CRUDKubernetesDependentResource<Secret, DevEnvironment> {
	private static final String LABEL_KEY = "devjoy.io/secret.type";
	private static final String LABEL_VALUE = "user";
	private static final String TOKEN_KEY = "token";
	private EnvironmentServiceImpl envService;
	private static final Logger LOG = LoggerFactory.getLogger(GiteaUserSecretDependentResource.class);
	private String username;
	
	public GiteaUserSecretDependentResource() {
		super(Secret.class);
	}
	
	public GiteaUserSecretDependentResource(EnvironmentServiceImpl envService, 
			String username, KubernetesClient client) {
		super(Secret.class);
		this.envService = envService;
		this.username = username;
		super.client = client;
	}

	@Override
	protected Secret desired(DevEnvironment primary, Context<DevEnvironment> context) {
		LOG.info("Setting desired state");
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("dev/gitea-user-secret.yaml")).get();
		desired.getMetadata().setName(username + desired.getMetadata().getName());
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put("user", Base64.encodeBytes(
				username.getBytes()));
		String password = "devjoy";
		desired.getData().put("password", Base64.encodeBytes(
				password.getBytes()));
		String giteaRoute = getRouteFromGitea(primary);
		desired.getData().put(".gitconfig", Base64.encodeBytes(
				String.format("[credential \"%s\"]\n"
						+ "\nhelper = store", giteaRoute).getBytes()));
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
			envService.replaceUserToken(username, password, primary).ifPresent(t -> {
				LOG.info("Updating token for secret {}", desired.getMetadata().getName());
				desired.getData().put(TOKEN_KEY, Base64.encodeBytes(t.getValue().getBytes()));
				desired.getData().put(".git-credentials", Base64.encodeBytes(
						getGitCredentials(username, t.getValue(), withoutProtocol(giteaRoute)).getBytes()));
			});
		}
		return desired;
	}

	private String withoutProtocol(String giteaRoute) {
		return giteaRoute.replaceAll(".+://", "");
	}

	private String getRouteFromGitea(DevEnvironment primary) {
		return GiteaDependentResource.getResource(client, primary)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getStatus().getGiteaRoute()), 10, TimeUnit.SECONDS)
				.getStatus().getGiteaRoute();
	}

	private void addOwnerReference(DevEnvironment primary, Secret desired) {
		List<OwnerReference> ownerReferences = new ArrayList<>();
		ownerReferences.add(new OwnerReference(primary.getApiVersion(), true, true, primary.getKind(), primary.getMetadata().getName(), primary.getMetadata().getUid()));
		desired.getMetadata().setOwnerReferences(ownerReferences);
	}
	
	public void reconcileDirectly(DevEnvironment primary, Context<DevEnvironment> context) {
		Secret desired = desired(primary, context);
		client.secrets().inNamespace(desired.getMetadata().getNamespace()).createOrReplace(desired);
	}
	
	private String getGitCredentials(String username, String token, String gitBaseUrl) {
		return String.format("https://%s:%s@%s", username, token, gitBaseUrl);
	}

	public static Resource<Secret> getResource(DevEnvironment primary, String username, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				username + "-git-secret");
	}
}
