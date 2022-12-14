package io.devjoy.operator.repository.service;

import java.util.Base64;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.repository.Gitea;
import io.devjoy.operator.repository.Github;
import io.devjoy.operator.repository.domain.GitProvider;
import io.devjoy.operator.repository.domain.RepositoryService;
import io.devjoy.operator.repository.k8s.Repository;
import io.devjoy.operator.repository.k8s.resources.GitSecretService;

@ApplicationScoped
public class GitServiceImpl {
	private final Map<GitProvider, RepositoryService> repoServices;
	private final GitSecretService secretService;
	private static Logger LOG = LoggerFactory.getLogger(GitServiceImpl.class);
	
	public GitServiceImpl(
			@Gitea RepositoryService gitea,
			@Github RepositoryService github,
			GitSecretService secretService
			) {
		repoServices = Map.of(GitProvider.GITEA, gitea, GitProvider.GITHUB, github);
		this.secretService = secretService;
	}
	
	public Optional<io.devjoy.operator.repository.domain.Repository> createIfRepositoryNotExists(String namespace, Repository repositoryResource) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		String token = getRepositoryToken(namespace, repository);
		RepositoryService repositoryService = repoServices.get(repository.getProvider());
		return Optional.ofNullable(repositoryService.		
				getByUserAndName(repository.getUser(), repository.getName(), token).orElseGet(() -> {
			LOG.info("Repository does not exist. Creating it...");
			repositoryService.create(repository, token);
			return null;
		}));
	}
	
	public void delete(Repository repositoryResource) {
		io.devjoy.operator.repository.domain.Repository repository = repositoryResource.toRepository();
		RepositoryService repositoryService = repoServices.get(repository.getProvider());
		repositoryService.delete(repository, getRepositoryToken(repositoryResource.getMetadata().getNamespace(), repository));
	}
	
	private String getRepositoryToken(String namespace, io.devjoy.operator.repository.domain.Repository repository) {
		return secretService.getByUser(namespace, repository.getUser()).map(s -> s.getData().get("password"))
				.map(s -> new String(Base64.getDecoder().decode(s))).orElseGet(() -> {
					LOG.warn("No secret found for user {} ", repository.getUser());
					return "";
				});
	}

}
