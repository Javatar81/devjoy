package io.devjoy.gitea.k8s.dependent.gitea;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.service.GiteaApiService;
import io.devjoy.gitea.service.ServiceException;
import io.devjoy.gitea.service.UserService;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.BooleanWithUndefined;
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
@KubernetesDependent(resourceDiscriminator = GiteaAdminSecretDiscriminator.class, labelSelector = GiteaAdminSecretDependent.SELECTOR)
public class GiteaAdminSecretDependent extends CRUDKubernetesDependentResource<Secret, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaAdminSecretDependent.class);
	public static final String DATA_KEY_USERNAME = "user";
	public static final String DATA_KEY_PASSWORD = "password";
	public static final String DATA_KEY_TOKEN = "token";
	public static final String DATA_KEY_GITCONFIG = ".gitconfig";
	public static final String DATA_KEY_GIT_CREDENTIALS = ".git-credentials";
	private static final String LABEL_TYPE_KEY = "devjoy.io/secret.type";
	private static final String LABEL_TYPE_VALUE = "admin";
	public static final String LABEL_TYPE_SELECTOR = LABEL_TYPE_KEY + "=" + LABEL_TYPE_VALUE;
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
	
	public GiteaAdminSecretDependent() {
		super(Secret.class);
		
	}

	public static String getName(Gitea primary) {
		return getName(primary.getSpec() != null ? primary.getSpec().getAdminUser() : "devjoyadmin");
	}
	
	public static String getName(String admin) {
		return admin.toLowerCase() + "-git-secret";
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
		passwordFromSecret.ifPresent(pw -> LOG.debug("Password available from secret"));	
		Optional<String> extraAdminSecretName = Optional.ofNullable(primary.getSpec() != null ? primary.getSpec().getExtraAdminSecretName() : null);
		String extraAdminSecretPasswordKey = primary.getSpec() != null ? primary.getSpec().getExtraAdminSecretPasswordKey() : DATA_KEY_PASSWORD;
		Optional<String> passwordFromExtraSecret =	extraAdminSecretName
			.flatMap(extraSecretName -> Optional.ofNullable(context.getClient().secrets().inNamespace(primary.getMetadata().getNamespace()).withName(extraSecretName).get()))
			.map(s -> s.getData().get(extraAdminSecretPasswordKey))
			.filter(pw -> !StringUtil.isNullOrEmpty(pw))
			.map(pw -> new String(Base64.getDecoder().decode(pw)));
		
		extraAdminSecretName.ifPresent(n -> LOG.debug("Extra secret name is {}", n));	
		passwordFromExtraSecret.ifPresent(pw -> LOG.debug("Password available from extra secret"));	
		if (passwordFromExtraSecret.isEmpty() && passwordFromSpec.isEmpty() && passwordFromSecret.isEmpty()) {
			LOG.info("No password set. Generating new one.");
			String newPassword = passwordService.generateNewPassword(Optional.ofNullable(primary.getSpec()).map(GiteaSpec::getAdminPasswordLength).orElse(10));
			if(primary.getSpec() == null) {
				primary.setSpec(new GiteaSpec());
			}
			primary.getSpec().setAdminPassword(newPassword);
			passwordFromSpec = Optional.of(newPassword);
		} 
		else if(!passwordFromSpec.isEmpty() && !passwordFromSecret.isEmpty() && !passwordFromSecret.equals(passwordFromSpec)) {
			LOG.info("Password changed.");
			passwordFromSpec.ifPresent(pw -> getSecondaryResource(primary, context)
					.flatMap(GiteaAdminSecretDependent::getAdminToken)
					.ifPresent(token -> userService.changeUserPassword(primary, adminUser, pw, token)));
		}
		
		//Optional<String> effectivePassword = passwordFromSpec.filter(pw -> !StringUtil.isNullOrEmpty(pw))
		//		.or(() -> passwordFromSecret.filter(pw -> !StringUtil.isNullOrEmpty(pw)));
		
		Optional<String> effectivePassword = passwordFromSpec.filter(pw -> !StringUtil.isNullOrEmpty(pw))
			.or(() -> passwordFromExtraSecret.filter(pw -> !StringUtil.isNullOrEmpty(pw)))
			.or(() -> passwordFromSecret.filter(pw -> !StringUtil.isNullOrEmpty(pw)));
		
		effectivePassword.ifPresent(pw -> desired.getData().put(DATA_KEY_PASSWORD, Base64.getEncoder().encodeToString(
				pw.getBytes())));
		
		
		HashMap<String, String> labels = new HashMap<>();
		labels.put(LABEL_KEY, LABEL_VALUE);
		labels.put(LABEL_TYPE_KEY, LABEL_TYPE_VALUE);
		desired.getMetadata().setLabels(labels);

		desiredToken(primary, context, desired, adminUser, effectivePassword);
		
		
		Optional<String> routeUrl = context.getSecondaryResource(Route.class).map(r -> String.format("%s://%s", "http" + (r.getSpec().getTls() != null ? "s": ""), r.getSpec().getHost()));
		routeUrl.ifPresent(url -> desired.getData().put(DATA_KEY_GITCONFIG, new String(Base64.getEncoder().encode(
				String.format("[credential \"%s\"]\n"
						+ "\nhelper = store", url).getBytes()))));
		
		
		return desired;
	}

	private void desiredToken(Gitea primary, Context<Gitea> context, Secret desired, String adminUser,
			Optional<String> effectivePassword) {
		//Replace the token because reconcile will call this again and we can't get the token anymore
		giteaApiService.getBaseUri(primary)
			.ifPresent(baseUri -> {
				Secret existingSecret = getResource(primary, context.getClient()).get();
				try {
					if (existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(DATA_KEY_TOKEN))) {
						LOG.info("Token already set. Taking it over from existing to desired.");
						desired.getData().put(DATA_KEY_TOKEN, existingSecret.getData().get(DATA_KEY_TOKEN));
					} else if(existingSecret != null && existingSecret.getData() != null && !StringUtil.isNullOrEmpty(existingSecret.getData().get(DATA_KEY_PASSWORD))){
						LOG.info("Token not set. Generating new token.");
						context.getSecondaryResource(Deployment.class, deploymentDiscriminator)
							.filter(d -> d.getStatus().getReadyReplicas() != null && d.getStatus().getReadyReplicas() > 0)
							.flatMap(d -> {
								LOG.info("Deployment replicas: '{}'", d.getStatus().getReadyReplicas());
								return effectivePassword;
							})
							.filter(pw -> !StringUtil.isNullOrEmpty(pw))
							.flatMap(pw -> userService.createAdminAccessToken(primary, adminUser, pw, "devjoy-" + primary.getMetadata().getName()))
							.ifPresentOrElse(
								t -> {
									LOG.info("Updating token for secret {}", desired.getMetadata().getName());
									desired.getData().put(DATA_KEY_TOKEN, Base64.getEncoder().encodeToString(t.getSha1().getBytes()));
									giteaApiService.getBaseUri(primary).ifPresent(uri -> 
										getGitCredentials(adminUser, t.getSha1(), uri).ifPresent(c -> 
											desired.getData().put(DATA_KEY_GIT_CREDENTIALS, new String(Base64.getEncoder().encode(c.getBytes())))
										)
									);
									}, 
								() -> LOG.warn("Cannot update token. Reasons could be: deployment not ready, or password is empty.")
							);
					} else {
						LOG.info("Secret is not yet created");	
					}
				} catch (WebApplicationException e) {
					throw new ServiceException("Error in API call", e);
				}
			});
	}
	
	private Optional<String> getGitCredentials(String username, String token, String gitBaseUrl) {
		try {
			URL url = new URIBuilder(gitBaseUrl).build().toURL();
			return Optional.of(String.format("%s://%s:%s@%s%s", url.getProtocol(), username, token, url.getHost(), url.getFile()));
		} catch (MalformedURLException | URISyntaxException e) {
			LOG.error("Invalid gitBaseUrl " + gitBaseUrl, e);
			return Optional.empty();
		}
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
