package io.devjoy.operator.environment.service;

import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.Config;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.environment.k8s.domain.TokenService;
import io.devjoy.operator.repository.domain.Token;
import io.devjoy.operator.repository.k8s.resources.GitSecretService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.util.StringUtil;

@ApplicationScoped
public class EnvironmentServiceImpl {
	private static final Logger LOG = LoggerFactory.getLogger(EnvironmentServiceImpl.class);
	@Inject
	Config config;
	private final TokenService tokenService;
	private final GitSecretService secretService;
	private final KubernetesClient client;
	
	public EnvironmentServiceImpl(TokenService tokenService, GitSecretService secretService, KubernetesClient client) {
		this.tokenService = tokenService;
		this.secretService = secretService;
		this.client = client;
	}
	
	public Optional<Token> createUserTokenIfNotExists(String userName, String password, DevEnvironment env) {
	return tokenService.getTokensByUser(userName, password, getRouteFromGitea(env)).stream()
		.filter(t -> config.getUserTokenName(userName).equals(t.getName()))
		.findFirst()
		.map(t -> {
			LOG.info("devjoy token exists for admin user {} ", userName);
			return t;
		})
		.or(() -> {
			LOG.info("devjoy token does not exist for admin user {}. Creating it.", userName);
			return tokenService.createTokenForUser(userName, password, config.getUserTokenName(userName), getRouteFromGitea(env));
		});
	}
	
	public Optional<Token> replaceUserToken(String userName, String password, DevEnvironment env) {
		return tokenService.getTokensByUser(userName, password, getRouteFromGitea(env)).stream()
				.filter(t -> config.getUserTokenName(userName).equals(t.getName()))
				.findFirst()
				.flatMap(t -> {
					LOG.info("Deleting devjoy token for user {} ", userName);
					tokenService.deleteTokenForUser(userName, password, t.getName(), getRouteFromGitea(env));
					LOG.info("Creating new devjoy token for user {} ", userName);
					return tokenService.createTokenForUser(userName, password, config.getUserTokenName(userName), getRouteFromGitea(env));
				})
				.or(() -> {
					LOG.info("devjoy token does not exist for user {}. Creating it.", userName);
					return tokenService.createTokenForUser(userName, password, config.getUserTokenName(userName), getRouteFromGitea(env));
				});
	}
	
	public Optional<Token> getUserToken(String namespace, String adminName, DevEnvironment env) {
		Optional<String> password = getUserPasswordFromSecret(namespace, adminName);
		if (!password.isPresent()) {
			LOG.warn("No secret found for user {}", adminName);
			return Optional.empty();
		} else {
			return tokenService.getTokensByUser(adminName, password.get(), getRouteFromGitea(env)).stream()
					.filter(t -> "devjoy".equals(t.getName()))
					.findFirst();
		}
	}
	
	public Optional<String> getUserPasswordFromSecret(String namespace, String userName) {
		return secretService.getByUser(namespace, userName).map(s -> s.getData().get("password"))
				.map(s -> new String(Base64.getDecoder().decode(s)));
	}
	
	private String getRouteFromGitea(DevEnvironment env) {
		return GiteaDependentResource.getResource(client, env)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getStatus().getGiteaRoute()), 10, TimeUnit.SECONDS)
				.getStatus().getGiteaRoute();
	}

}
