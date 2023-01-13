package io.devjoy.operator.repository.service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.GiteaDependentResource;
import io.devjoy.operator.repository.Gitea;
import io.devjoy.operator.repository.Github;
import io.devjoy.operator.repository.domain.GitProvider;
import io.devjoy.operator.repository.domain.RepositoryService;
import io.devjoy.operator.repository.domain.UserService;
import io.devjoy.operator.repository.k8s.Repository;
import io.devjoy.operator.repository.k8s.resources.GitSecretService;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.runtime.util.StringUtil;

@ApplicationScoped
public class GitServiceImpl {
	private static final Logger LOG = LoggerFactory.getLogger(GitServiceImpl.class);
	private final Map<GitProvider, RepositoryService> repoServices;
	private final Map<GitProvider, UserService> userServices;
	private final GitSecretService secretService;
	private final KubernetesClient client;
	
	public GitServiceImpl(
			@Gitea RepositoryService gitea,
			@Github RepositoryService github,
			@Gitea UserService giteaUserService,
			GitSecretService secretService,
			KubernetesClient client
			) {
		repoServices = Map.of(GitProvider.GITEA, gitea, GitProvider.GITHUB, github);
		userServices = Map.of(GitProvider.GITEA, giteaUserService);
		this.secretService = secretService;
		this.client = client;
	}
	
	public boolean repoUserExists(String repoUser, String namespace, Repository repositoryResource, String adminUser, DevEnvironment env) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		String token = getRepositoryTokenFromSecret(namespace, adminUser);
		return userServices.get(repository.getProvider()).getAllUsernames(token, getRouteFromGitea(env)).contains(repoUser);
	}
	
	public void createRepoUser(String repoUser, String reposUserEmail, String namespace, Repository repositoryResource, String adminUser, DevEnvironment env) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		String token = getRepositoryTokenFromSecret(namespace, adminUser);
		userServices.get(repository.getProvider()).createUser(repoUser, reposUserEmail, token, getRouteFromGitea(env));
	}
	
	public Optional<io.devjoy.operator.repository.domain.Repository> createIfRepositoryNotExists(String namespace, Repository repositoryResource, String adminUser, DevEnvironment env) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		String token = getRepositoryTokenFromSecret(namespace, adminUser);
		String route = getRouteFromGitea(env);
		RepositoryService repositoryService = repoServices.get(repository.getProvider());
		return Optional.ofNullable(repositoryService.		
				getByUserAndName(repository.getUser(), repository.getName(), token, route).orElseGet(() -> {
			LOG.info("Repository for user {} does not exist. Creating it... {}", repository.getUser(), token);
			repositoryService.create(repository, token, route);
			return null;
		}));
	}
	
	public void delete(Repository repositoryResource, String adminUser, DevEnvironment env) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		RepositoryService repositoryService = repoServices.get(repository.getProvider());
		repositoryService.delete(repository, getRepositoryTokenFromSecret(repositoryResource.getMetadata().getNamespace(), adminUser), getRouteFromGitea(env));
	}
	
	private String getRepositoryTokenFromSecret(String namespace, String user) {
		return secretService.getByUser(namespace, user).map(s -> s.getData().get("token"))
				.map(s -> "token " + new String(Base64.getDecoder().decode(s)).trim()).orElseGet(() -> {
					LOG.warn("No secret found for user {} ", user);
					return "";
				});
	}
	
	private String getRouteFromGitea(DevEnvironment env) {
		return GiteaDependentResource.getResource(client, env)
				.waitUntilCondition(c -> !StringUtil.isNullOrEmpty(c.getStatus().getGiteaRoute()), 10, TimeUnit.SECONDS)
				.getStatus().getGiteaRoute();
	}

}
