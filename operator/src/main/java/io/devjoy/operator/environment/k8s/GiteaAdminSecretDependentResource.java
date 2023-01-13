package io.devjoy.operator.environment.k8s;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.service.EnvironmentServiceImpl;
import io.devjoy.operator.repository.k8s.resources.GitSecretService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.Base64;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

/**
 * This class represents the secret resource to store the username, password and token of the Gitea admin user.
 * Since it is not possible with Gitea to retrieve tokens after generation, the token is regenerated if it is 
 * not stored in this secret. 
 *
 */
@KubernetesDependent(labelSelector = GiteaAdminSecretDependentResource.LABEL_SELECTOR)
public class GiteaAdminSecretDependentResource extends CRUDKubernetesDependentResource<Secret, DevEnvironment> {
	
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretDependentResource.class);
	private static final String TOKEN_KEY = "token";
	private static final String LABEL_KEY = "devjoy.io/secret.type";
	private static final String LABEL_VALUE = "admin";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	@Inject
	EnvironmentServiceImpl envService;
	@Inject
	GitSecretService secretService;
	
	public GiteaAdminSecretDependentResource() {
		super(Secret.class);
		
	}

	@Override
	protected Secret desired(DevEnvironment primary, Context<DevEnvironment> context) {
		LOG.debug("Setting desired state");
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("dev/gitea-admin-secret.yaml")).get();
		String adminUser = getAdminUser(primary, client);
		String password = getAdminPassword(primary);
		desired.getMetadata().setName(adminUser + desired.getMetadata().getName());
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put("user", Base64.encodeBytes(
				adminUser.getBytes()));
		desired.getData().put("password", Base64.encodeBytes(
				password.getBytes()));
		
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		desired.getMetadata().setLabels(labels);
		
		//Replace the token because reconcile will call this again and we can't get the token anymore
		Secret existingSecret = getResource(primary, client).get();
		if (existingSecret != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(TOKEN_KEY))) {
			LOG.info("Token already set. Taking it over from existing to desired.");
			desired.getData().put(TOKEN_KEY, existingSecret.getData().get(TOKEN_KEY));
		} else {
			LOG.info("Token not set. Generating new token.");
			envService.replaceUserToken(adminUser, password, primary).ifPresent(t -> {
				desired.getData().put(TOKEN_KEY, Base64.encodeBytes(t.getValue().getBytes()));
				LOG.info("Updated token for secret {}", desired.getMetadata().getName());
			});
		}
		return desired;
	}

	private String getAdminPassword(DevEnvironment primary) {
		return GiteaDependentResource.getResource(client, primary)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getStatus().getAdminPassword()), 30, TimeUnit.SECONDS)
				.getStatus().getAdminPassword();
	}

	private static String getAdminUser(DevEnvironment primary, KubernetesClient client) {
		return GiteaDependentResource.getResource(client, primary)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getSpec().getGiteaAdminUser()), 10, TimeUnit.SECONDS)
				.getSpec().getGiteaAdminUser();
	}

	static Resource<Secret> getResource(DevEnvironment primary, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				getAdminUser(primary, client) + "-git-secret");
	}
}
