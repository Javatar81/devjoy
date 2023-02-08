package io.devjoy.gitea.repository.domain;

import static io.devjoy.gitea.repository.domain.Visibility.PRIVATE;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.repository.api.RepoService;
import io.devjoy.gitea.repository.k8s.GiteaRepository;


@ApplicationScoped
public class RepositoryService {
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

	public Repository createIfNotExists(GiteaRepository repository, String token, String baseUri) {
		return getByUserAndName(repository.getSpec().getUser(), repository.getMetadata().getName(), token, baseUri)
			.orElseGet(() -> create(repository, token, baseUri));
	}
	
	
	
	public Repository create(GiteaRepository repository, String token, String baseUri) {
		try {
			LOG.info("Creating repository {}", repository);
			CreateRepoOption repoOption = new CreateRepoOption();
			repoOption.setName(repository.getMetadata().getName());
			repoOption.setPrivate(repository.getSpec().getVisibility() == PRIVATE);
			repoOption.setDefaultBranch("main");
			return getDynamicUrlClient(baseUri).create(token, repoOption);
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException | WebApplicationException e) {
			throw new ServiceException(e);
		}
	}

	public Optional<Repository> getByUserAndName(String user, String name, String token, String baseUri) {
		try {
			RepoService repoService = getDynamicUrlClient(baseUri);
			return Optional.ofNullable(repoService.getByUserAndName(token, user, name));
				
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == 404) {
				return Optional.empty();
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
			return Optional.empty();
		}
	}

	public void delete(GiteaRepository repository, String token, String baseUri) {
		try {
			RepoService repoService = getDynamicUrlClient(baseUri);
			repoService.deleteByUserAndName(token, repository.getSpec().getUser(), repository.getMetadata().getName());
		} catch (URISyntaxException e) {
			LOG.error("Error calling repository API to delete user", e);
		}
		
	}
	
	private RepoService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(RepoService.class);
	}
}
