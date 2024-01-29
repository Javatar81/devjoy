package io.devjoy.gitea.domain.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.Command;
import io.devjoy.gitea.domain.Option;
import io.devjoy.gitea.k8s.dependent.rhsso.Keycloak;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClient;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class AuthenticationService {
	private static final String ARG_AUTH = "auth";
	private static final String ARG_ADMIN = "admin";
	private static final Logger LOG = LoggerFactory.getLogger(AuthenticationService.class);
	private final KubernetesClient client;
	private final GiteaPodExecService execService;
	
	public AuthenticationService(KubernetesClient client, GiteaPodExecService execService) {
		super();
		this.client = client;
		this.execService = execService;
	}
	
	public Optional<String> getAuthenticationSourceId(Gitea gitea) {
		LOG.info("Waiting for Keycloak to become ready");
		Optional<Keycloak> keycloak = Optional.ofNullable(KeycloakDependentResource.getResource(gitea, client).waitUntilCondition(c -> c!= null && c.getStatus() != null && !StringUtil.isNullOrEmpty(c.getStatus().getExternalURL()), 180, TimeUnit.SECONDS));
		LOG.info("Keycloak ready? {}", keycloak.isPresent());
		LOG.info("Waiting for KeycloakClient to become ready");
		Optional<KeycloakClient> keycloakClient = Optional.ofNullable(KeycloakClientDependentResource.getResource(gitea, client).waitUntilCondition(c -> c!= null && c.getStatus() != null && c.getStatus().getReady(), 60, TimeUnit.SECONDS));
		LOG.info("KeycloakClient ready? {}", keycloakClient.isPresent());
		// /usr/bin/giteacmd admin auth list"
		Command cmd = Command.builder()
				.withExecutable("/usr/bin/giteacmd")
				.withArgs(List.of(ARG_ADMIN, ARG_AUTH, "list")).build();
			
		Pattern pattern = Pattern.compile(String.format("(.+?)\t%s.*", oauthName(gitea)));
		return execService.execOnDeployment(gitea, cmd)
				.map(pattern::matcher)
				.filter(Matcher::find)
				.map(m -> m.group(1));
	}

	public void registerAuthenticationSource(Gitea gitea) {
		LOG.info("Registering new auth source");
		doExecute(gitea, List.of(ARG_ADMIN, ARG_AUTH, "add-oauth"), Collections.emptyList());
		gitea.getStatus().getConditions().add(new ConditionBuilder()
				.withObservedGeneration(gitea.getStatus().getObservedGeneration())
				.withType(GiteaConditionType.GITEA_ADMIN_PW_IN_SECRET.toString())
				.withMessage("Created authentication source via cli.")
				.withLastTransitionTime(LocalDateTime.now().toString())
				.withReason("SSO is enabled. Triggers OpenIdConnect setup for Gitea.")
				.withStatus("true")
				.build());
	}
	
	public void updateAuthenticationSource(Gitea gitea, String id) {
		LOG.info("Updating auth source");
		doExecute(gitea, List.of(ARG_ADMIN, ARG_AUTH, "update-oauth"), List.of(new Option("id", id)));
	}

	private void doExecute(Gitea gitea, List<String> args, List<Option> options) {
		LOG.info("Waiting for Keycloak to become ready");
		Optional<Keycloak> keycloak = Optional.ofNullable(KeycloakDependentResource.getResource(gitea, client).waitUntilCondition(c -> c!= null && c.getStatus() != null && !StringUtil.isNullOrEmpty(c.getStatus().getExternalURL()), 180, TimeUnit.SECONDS));
		LOG.info("Keycloak ready? {}", keycloak.isPresent());
		LOG.info("Waiting for KeycloakClient to become ready");
		Optional<KeycloakClient> keycloakClient = Optional.ofNullable(KeycloakClientDependentResource.getResource(gitea, client).waitUntilCondition(c -> c!= null && c.getStatus() != null && c.getStatus().getReady(), 60, TimeUnit.SECONDS));
		LOG.info("KeycloakClient ready? {}", keycloakClient.isPresent());
		String realm = KeycloakRealmDependentResource.resourceName(gitea);
		
		keycloakClient.ifPresent(c -> 
			keycloak.map(k -> 
				Command.builder()
				.withExecutable("/usr/bin/giteacmd")
				.addArgs(args)
				.addOptions(options)
				.addOption(new Option("name", oauthName(gitea)))
				.addOption(new Option("provider", "openidConnect"))
				.addOption(new Option("key", KeycloakClientDependentResource.clientId(gitea)))
				.addOption(new Option("secret", c.getSpec().getClient().getSecret()))
				.addOption(new Option("auto-discover-url", k.getStatus().getExternalURL() + discoveryPath(realm)))
				.addOption(new Option("scopes", "email profile"))
				.build()
			).ifPresent(cmd -> execService.execOnDeployment(gitea, cmd))
		);
	}
	
	public String oauthName(Gitea gitea) {
		return gitea.getMetadata().getName() + "-devjoy-oidc";
	}

	private String discoveryPath(String realm) {
		return String.format("/auth/realms/%s/.well-known/openid-configuration", realm);
	}
}
