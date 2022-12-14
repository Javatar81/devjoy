package io.devjoy.operator.repository.github;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;

import io.devjoy.operator.repository.Github;
import io.devjoy.operator.repository.domain.Repository;
import io.devjoy.operator.repository.domain.RepositoryService;

@ApplicationScoped
@Github
public class GithubRepoServiceAdapter implements RepositoryService {

	@Inject
    @RestClient
    RepoService repoService;

	@Override
	public Repository create(Repository repository, String token) {
		return repoService.create("token " + token, new CreateRepoInput(repository)).toRepository();
	}

	@Override
	public Optional<Repository> getByUserAndName(String user, String name, String token) {
		try {
			return Optional.ofNullable(repoService.getByUserAndName("Bearer " + token, user, name))
				.map(GetRepoOutput::toRepository);
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				return Optional.empty();
			} else {
				throw e;
			}
		}
	}

	@Override
	public void delete(Repository repository, String token) {
		repoService.deleteByUserAndName("Bearer " + token, repository.getUser(), repository.getName());
	}

}
