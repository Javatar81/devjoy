package io.devjoy.gitea.k8s.gitea;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ApiAccessMode;
import io.devjoy.gitea.domain.GiteaApiService;
import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/**
 * This class represents the secret resource to store the username, password and token of the Gitea admin user.
 * Since it is not possible with Gitea to retrieve tokens after generation, the token is regenerated if it is 
 * not stored in this secret. 
 *
 */
@KubernetesDependent(resourceDiscriminator = GiteaAdminSecretDiscriminator.class, labelSelector = GiteaAdminSecretDependentResource.SELECTOR)
public class GiteaAdminSecretDependentResource extends CRUDKubernetesDependentResource<Secret, Gitea> {
	
	private static final String DATA_KEY_PASSWORD = "password";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretDependentResource.class);
	private static final String TOKEN_KEY = "token";
	private static final String LABEL_TYPE_KEY = "devjoy.io/secret.type";
	private static final String LABEL_TYPE_VALUE = "admin";
	static final String LABEL_TYPE_SELECTOR = LABEL_TYPE_KEY + "=" + LABEL_TYPE_VALUE;
	private static final String LABEL_KEY = "devjoy.io/secret.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	static final String SELECTOR = LABEL_SELECTOR + "," + LABEL_TYPE_SELECTOR;
	@Inject
	TokenService tokenService;
	@Inject
	GiteaApiService giteaApiService;
	
	
	@ConfigProperty(name = "io.devjoy.gitea.api.access.mode") 
	ApiAccessMode accessMode;
	
	public GiteaAdminSecretDependentResource() {
		super(Secret.class);
		
	}

	public static String getName(Gitea primary) {
		return primary.getSpec().getAdminUser().toLowerCase() + "-git-secret";
	}

	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.debug("Setting desired state");
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/admin-secret.yaml")).item();
		String adminUser = primary.getSpec().getAdminUser();
		String adminPassword = primary.getSpec().getAdminPassword();
		desired.getMetadata().setName(getName(primary));
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put("user", new String(Base64.getEncoder().encode(
				adminUser.getBytes())));
		
		Optional.ofNullable(getResource(primary, client).get())
			.map(s -> s.getData().get(DATA_KEY_PASSWORD))
			.ifPresentOrElse(pw -> desired.getData().put(DATA_KEY_PASSWORD, pw),
				() -> {
					if (!StringUtil.isNullOrEmpty(adminPassword)) {
						LOG.info("Storing admin password from Gitea spec.");
						desired.getData().put(DATA_KEY_PASSWORD, new String(Base64.getEncoder().encode(
								adminPassword.getBytes())));
					} else {
						LOG.warn("No password available. Will set it later");
					}
				}
			);
		
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		labels.put(LABEL_TYPE_KEY, LABEL_TYPE_VALUE);
		desired.getMetadata().setLabels(labels);

		//Replace the token because reconcile will call this again and we can't get the token anymore
		giteaApiService.getBaseUri(primary)
			.ifPresent(baseUri -> {
				Secret existingSecret = getResource(primary, client).get();
				try {
					
					if (existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(TOKEN_KEY))) {
						LOG.info("Token already set. Taking it over from existing to desired.");
						desired.getData().put(TOKEN_KEY, existingSecret.getData().get(TOKEN_KEY));
					} else if(existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(DATA_KEY_PASSWORD))){
						//String password = new String(Base64.getDecoder().decode(existingSecret.getData().get(DATA_KEY_PASSWORD)));
						LOG.info("Token not set. Generating new token.");
						tokenService.createUserTokenViaCli(primary, adminUser, "devjoy-" + primary.getMetadata().getNamespace())
						.ifPresentOrElse(t -> {
							LOG.info("Updating token for secret {}", desired.getMetadata().getName());
							desired.getData().put(TOKEN_KEY, new String(Base64.getEncoder().encode(t.getBytes())));
						}, () -> LOG.warn("Cannot update token."));
						
						/*AccessToken token = tokenService.replaceUserToken(baseUri, adminUser, password);
						LOG.info("Token is {}", token.getSha1());
						desired.getData().put(TOKEN_KEY, new String(Base64.getEncoder().encode(token.getSha1().getBytes())));
						LOG.info("Updated token for secret {}", desired.getMetadata().getName());		*/
					} else {
						LOG.info("Secret is not yet created");	
					}
				} catch (WebApplicationException e1) {
					LOG.error("Error setting token data", e1);
				}
			});
		return desired;
	}

	

	public static Resource<Secret> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				getName(primary));
	}
}
