package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
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
	public static final String DATA_KEY_USERNAME = "user";
	public static final String DATA_KEY_PASSWORD = "password";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretDependentResource.class);
	public static final String DATA_KEY_TOKEN = "token";
	private static final String LABEL_TYPE_KEY = "devjoy.io/secret.type";
	private static final String LABEL_TYPE_VALUE = "admin";
	static final String LABEL_TYPE_SELECTOR = LABEL_TYPE_KEY + "=" + LABEL_TYPE_VALUE;
	private static final String LABEL_KEY = "devjoy.io/secret.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	static final String SELECTOR = LABEL_SELECTOR + "," + LABEL_TYPE_SELECTOR;
	
	@Inject
	PasswordService passwordService;
	@Inject
	UserService userService;
	@Inject
	GiteaApiService giteaApiService;
	GiteaDeploymentDiscriminator deploymentDiscriminator = new GiteaDeploymentDiscriminator();
	
	public GiteaAdminSecretDependentResource() {
		super(Secret.class);
		
	}

	public static String getName(Gitea primary) {
		return (primary.getSpec() != null ? primary.getSpec().getAdminUser().toLowerCase() : "devjoyadmin") + "-git-secret";
	}

	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.debug("Setting desired state");
		Secret desired = context.getClient().resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/admin-secret.yaml")).item();
		String adminUser = primary.getSpec() != null ? primary.getSpec().getAdminUser() : "devjoyadmin";
		Optional<String> passwordFromSpec = Optional.ofNullable(primary.getSpec()).map(GiteaSpec::getAdminPassword);
		desired.getMetadata().setName(getName(primary));
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put(DATA_KEY_USERNAME, new String(Base64.getEncoder().encode(
				adminUser.getBytes())));
		
		Optional<String> passwordFromSecret = getSecondaryResource(primary, context)
			.map(s -> s.getData().get(DATA_KEY_PASSWORD))
			.filter(pw -> !StringUtil.isNullOrEmpty(pw))
			.map(pw -> new String(Base64.getDecoder().decode(pw)));
		
		LOG.info("Secret pw {}", passwordFromSecret);
		if (passwordFromSpec.isEmpty() && passwordFromSecret.isEmpty()) {
			LOG.info("No password set. Generating new one.");
			String newPassword = passwordService.generateNewPassword(Optional.ofNullable(primary.getSpec()).map(GiteaSpec::getAdminPasswordLength).orElse(10));
			if(primary.getSpec() == null) {
				primary.setSpec(new GiteaSpec());
			}
			primary.getSpec().setAdminPassword(newPassword);
			passwordFromSpec = Optional.of(newPassword);
		}
		
		
	
		/*Optional<String> effectivePassword;
		if (!passwordFromSpec.equals(passwordFromSecret)) {
			effectivePassword = passwordFromSpec
				.filter(pw -> !StringUtil.isNullOrEmpty(pw))
				.map(pw -> {
					LOG.info("Taking over password from Gitea spec {}. {}",pw, pw.length());
					return pw;
				});
		} else {
			// spec: pw secret: pw
			LOG.info("Password has not changed.");
			effectivePassword = passwordFromSpec;
			// We must state that desired == actual otherwise password is set to null
			
		}*/
		Optional<String> effectivePassword = passwordFromSpec.filter(pw -> !StringUtil.isNullOrEmpty(pw))
				.or(() -> passwordFromSecret.filter(pw -> !StringUtil.isNullOrEmpty(pw)));
		
		effectivePassword.ifPresent(pw -> desired.getData().put(DATA_KEY_PASSWORD, Base64.getEncoder().encodeToString(
				pw.getBytes())));
		
		
		
		/**Optional<String> password = passwordFromSecret
			.map(pw -> {
				LOG.info("Password .");
				desired.getData().put(DATA_KEY_PASSWORD, pw);
				return pw;
			}).or(
				() -> {
					if (!StringUtil.isNullOrEmpty(passwordFromSpec)) {
						LOG.info("Storing admin password from Gitea spec.");
						desired.getData().put(DATA_KEY_PASSWORD, new String(Base64.getEncoder().encode(
								passwordFromSpec.getBytes())));
						return Optional.of(passwordFromSpec);
					} else {
						LOG.warn("No password available. Will set it later");
						return Optional.empty();
					}
				}
			);**/
		
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		labels.put(LABEL_TYPE_KEY, LABEL_TYPE_VALUE);
		desired.getMetadata().setLabels(labels);

		//Replace the token because reconcile will call this again and we can't get the token anymore
		giteaApiService.getBaseUri(primary)
			.ifPresent(baseUri -> {
				Secret existingSecret = getResource(primary, context.getClient()).get();
				try {
					if (existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(DATA_KEY_TOKEN))) {
						LOG.info("Token already set. Taking it over from existing to desired.");
						desired.getData().put(DATA_KEY_TOKEN, existingSecret.getData().get(DATA_KEY_TOKEN));
					} else if(existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(DATA_KEY_PASSWORD))){
						//String password = new String(Base64.getDecoder().decode(existingSecret.getData().get(DATA_KEY_PASSWORD)));
						LOG.info("Token not set. Generating new token.");
						//tokenService.createAdminTokenViaCli(primary, adminUser, "devjoy-" + primary.getMetadata().getNamespace())
						context.getSecondaryResource(Deployment.class, deploymentDiscriminator)
							.filter(d -> d.getStatus().getReadyReplicas() != null && d.getStatus().getReadyReplicas() > 0)
							.flatMap(d -> {
								LOG.info("Deployment replicas: '{}'", d.getStatus().getReadyReplicas());
								return effectivePassword;
							})
							.filter(pw -> !StringUtil.isNullOrEmpty(pw))
							.flatMap(pw -> userService.createAdminAccessToken(primary, adminUser, pw, "devjoy-" + primary.getMetadata().getNamespace()))
							.ifPresentOrElse(
								t -> {
									LOG.info("Updating token for secret {}", desired.getMetadata().getName());
									desired.getData().put(DATA_KEY_TOKEN, Base64.getEncoder().encodeToString(t.getSha1().getBytes()));
									}, 
								() -> LOG.warn("Cannot update token.")
							);
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
	
	public static Optional<String> getAdminToken(Secret adminSecret) {
		return getNonEmptyValue(adminSecret, DATA_KEY_TOKEN);
	}
	
	public static Optional<String> getAdminPassword(Secret adminSecret) {
		return getNonEmptyValue(adminSecret, DATA_KEY_PASSWORD);
	}
	
	private static Optional<String> getNonEmptyValue(Secret adminSecret, String key) {
		return Optional.ofNullable(adminSecret.getData().get(key))
			.map(t -> new String(Base64.getDecoder().decode(t)).trim())
			.filter(p -> !StringUtil.isNullOrEmpty(p)); 
	}
}
