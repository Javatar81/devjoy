package io.devjoy.operator.repository.gitea;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.repository.Gitea;
import io.devjoy.operator.repository.domain.Repository;
import io.devjoy.operator.repository.domain.RepositoryService;

@ApplicationScoped
@Gitea
public class RepoServiceAdapter implements RepositoryService {

	private static final Logger LOG = LoggerFactory.getLogger(RepoServiceAdapter.class);
	@Override
	public Repository create(Repository repository, String token, String baseUri) {
		try {
			LOG.info("Creating repository {}", repository);
			return getDynamicUrlClient(baseUri).createByAdmin(token, repository.getUser(), new CreateRepoInput(repository)).toRepository();
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Optional<Repository> getByUserAndName(String user, String name, String token, String baseUri) {
		try {
			RepoService repoService = getDynamicUrlClient(baseUri);
			return Optional.ofNullable(repoService.getByUserAndName(token, user, name))
				.map(GetRepoOutput::toRepository);
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				return Optional.empty();
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
			return Optional.empty();
		}
	}

	@Override
	public void delete(Repository repository, String token, String baseUri) {
		try {
			RepoService repoService = getDynamicUrlClient(baseUri);
			repoService.deleteByUserAndName(token, repository.getUser(), repository.getName());
		} catch (URISyntaxException e) {
			LOG.error("Error calling repository API to delete user", e);
		}
		
	}
	
	private RepoService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(RepoService.class);
	}

}
