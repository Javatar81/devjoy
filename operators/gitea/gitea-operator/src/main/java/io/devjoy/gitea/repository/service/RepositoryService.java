package io.devjoy.gitea.repository.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.api.RepositoryApi;
import org.openapi.quarkus.gitea_json.model.CreateHookOption;
import org.openapi.quarkus.gitea_json.model.CreateHookOption.TypeEnum;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.Hook;
import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.service.ServiceException;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositoryConditionType;
import io.devjoy.gitea.repository.k8s.model.SecretReferenceSpec;
import io.devjoy.gitea.repository.k8s.model.WebhookSpec;
import io.devjoy.gitea.util.AuthorizationRequestFilter;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.constraints.NotEmpty;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.UriBuilder;


@ApplicationScoped
public class RepositoryService {
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

	/*public void createWebHooks(GiteaRepository repository, Map<SecretReferenceSpec, String> secrets, String token, String baseUri) {
		try {
			RepoService dynamicUrlClient = getDynamicUrlClient(baseUri);
			var hooks = dynamicUrlClient.getHooks(token, repository.getSpec().getUser(), repository.getMetadata().getName()).stream()
				.collect(Collectors.toList());
			repository.getSpec().getWebhooks().stream()
				.filter(w -> hooks.stream().noneMatch(h -> hookEquals(h, w)))
				.forEach(w -> {
					LOG.info("Creating hook");
					CreateHookOption hookOption = new CreateHookOption();
					hookOption.setActive(w.isActive());
					hookOption.setBranchFilter(w.getBranchFilter());
					hookOption.setType(TypeEnum.valueOf(w.getType()));
					hookOption.setEvents(w.getEvents());
						hookOption.setConfig(
								Map.of("content_type", "json", 
										"url", w.getTargetUrl(), 
										"secret", secrets.get(w.getSecretRef())));
					dynamicUrlClient.createHook(token, repository.getSpec().getUser(),
							repository.getMetadata().getName(), hookOption);
			});
		} catch (URISyntaxException e) {
			LOG.error("Error creating web hook", e);
		}
	}*/
	
	public void createWebHooks(GiteaRepository repository, Map<SecretReferenceSpec, String> secrets, String token, String baseUri) {
		try {
			RepositoryApi repoApi = getDynamicUrlClient(new URIBuilder(baseUri).build(), RepositoryApi.class, token);
			var hooks = repoApi.repoListHooks(repository.getSpec().getUser(), repository.getMetadata().getName(), 0, 1000);
			repository.getSpec().getWebhooks().stream()
				.filter(w -> hooks.stream().noneMatch(h -> hookEquals(h, w)))
				.forEach(w -> {
					LOG.info("Creating hook");
					CreateHookOption hookOption = new CreateHookOption();
					hookOption.setActive(w.isActive());
					hookOption.setBranchFilter(w.getBranchFilter());
					hookOption.setType(TypeEnum.valueOf(w.getType()));
					hookOption.setEvents(w.getEvents());
					hookOption.setConfig(
							Map.of("content_type", "json", 
									"url", w.getTargetUrl(), 
									"secret", secrets.get(w.getSecretRef())));
					repoApi.repoCreateHook(repository.getSpec().getUser(),
						repository.getMetadata().getName(), hookOption);
			});
		} catch (URISyntaxException e) {
			LOG.error("Error creating web hook", e);
		}
	}
	
	private boolean hookEquals(Hook a, WebhookSpec b) {
		return a.getType().equalsIgnoreCase(b.getType()) 
				&& Objects.equals(a.getConfig().get("url"), b.getTargetUrl()) 
				&& Objects.equals(a.getEvents(), b.getEvents());
	}
		
	/*public Repository createIfNotExists(GiteaRepository repository, String token, String baseUri) {
		return getByUserAndName(repository.getSpec().getUser(), repository.getMetadata().getName(), token, baseUri)
			.orElseGet(() -> {
				Repository repo = create(repository, token, baseUri);
				repository.getStatus().getConditions().add(new ConditionBuilder()
						.withObservedGeneration(repository.getStatus().getObservedGeneration())
						.withType(GiteaRepositoryConditionType.GITEA_REPO_CREATED.toString())
						.withMessage("Created repository via api.")
						.withLastTransitionTime(LocalDateTime.now().toString())
						.withReason("Repository did not exist.")
						.withStatus("true")
						.build());
				return repo;
			});
	}*/
	
	public Repository createIfNotExists(GiteaRepository repository, String token, String baseUri) {
		return getByUserAndName(repository.getSpec().getUser(), repository.getMetadata().getName(), token, baseUri)
			.orElseGet(() -> {
				return create(repository, token, baseUri);
			});
	}
	
	/*public Repository create(GiteaRepository repository, @NotEmpty String token, @NotEmpty String baseUri) {
		try {
			LOG.info("Creating repository {}", repository.getMetadata().getName());
			CreateRepoOption repoOption = new CreateRepoOption();
			repoOption.setName(repository.getMetadata().getName());
			repoOption.setPrivate(repository.getSpec().getVisibility() == Visibility.PRIVATE);
			repoOption.setDefaultBranch("main");
			return getDynamicUrlClient(baseUri).create(token, repoOption);
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException | WebApplicationException e) {
			throw new ServiceException(String.format("Error creating repository %s for user %s", repository.getMetadata().getName(), repository.getSpec().getUser()), e);
		}
	}*/
	
	public Repository create(GiteaRepository repository, @NotEmpty String token, @NotEmpty String baseUri) {
		LOG.info("Creating repository {}", repository.getMetadata().getName());
		CreateRepoOption repoOption = new CreateRepoOption();
		repoOption.setName(repository.getMetadata().getName());
		repoOption.setPrivate(repository.getSpec().getVisibility() == Visibility.PRIVATE);
		repoOption.setDefaultBranch("main");
		try {
			Repository repo = getDynamicUrlClient(new URIBuilder(baseUri).build(), AdminApi.class, token).adminCreateRepo(repository.getSpec().getUser(), repoOption);
			repository.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(repository.getStatus().getObservedGeneration())
					.withType(GiteaRepositoryConditionType.GITEA_REPO_CREATED.toString())
					.withMessage("Created repository via api.")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason("Repository did not exist.")
					.withStatus("true")
					.build());
			return repo;
		} catch (URISyntaxException e) {
			throw new ServiceException(e);
		}
	}

	public Optional<Repository> getByRepo(GiteaRepository repository, String token) {
		try {
			return getByUserAndName(repository.getSpec().getUser(), repository.getMetadata().getName(), token, getBaseUri(repository));
		} catch (URISyntaxException | MalformedURLException e) {
			LOG.error("Error calling repository API to delete user", e);
			return Optional.empty();
		}
	}

	/*public Optional<Repository> getByUserAndName(String user, String name, String token, String baseUri) {
		try {
			RepoService repoService = getDynamicUrlClient(baseUri);
			return Optional.ofNullable(repoService.getByUserAndName(token, user, name));
				
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == 404) {
				return Optional.empty();
			} else {
				throw new ServiceException("Repository cannot be found via API", e);
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
			return Optional.empty();
		}
	}*/
	public Optional<Repository> getByUserAndName(String user, String repoName, String token, String baseUri) {
		try {
			return Optional.ofNullable(getDynamicUrlClient(new URIBuilder(baseUri).build(), RepositoryApi.class, token).repoGet(user, repoName));	
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == 404) {
				return Optional.empty();
			} else {
				throw new ServiceException("Repository cannot be found via API", e);
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
			return Optional.empty();
		}
	}
	

	/*public void delete(GiteaRepository repository, String token) {
		try {
			String baseUri = getBaseUri(repository);
			RepoService repoService = getDynamicUrlClient(baseUri);
			repoService.deleteByUserAndName(token, repository.getSpec().getUser(), repository.getMetadata().getName());
		} catch (URISyntaxException | MalformedURLException e) {
			LOG.error("Error calling repository API to delete user", e);
		}
	}*/
	
	public void delete(GiteaRepository repository, String token) {
		try {
			String baseUri = getBaseUri(repository);
			getDynamicUrlClient(new URIBuilder(baseUri).build(), RepositoryApi.class, token).repoDelete(repository.getSpec().getUser(), repository.getMetadata().getName());
		} catch (URISyntaxException | MalformedURLException e) {
			LOG.error("Error calling repository API to delete user", e);
		}
	}

	private String getBaseUri(GiteaRepository repository) throws URISyntaxException, MalformedURLException {
		URL cloneUrl = URI.create(repository.getStatus().getCloneUrl()).toURL();
		String port = cloneUrl.getPort() > -1 ? ":" + cloneUrl.getPort() : ":";
		return String.format("%s://%s%s", cloneUrl.getProtocol(), cloneUrl.getHost(), port);
	}
	
	private <T> T getDynamicUrlClient(URI baseUri, Class<T> clazz, String token) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URIBuilder(baseUri).setPath("/api/v1").build())
				.register(AuthorizationRequestFilter.accessToken(token))
				.build(clazz);
	}
	
	/*private RepoService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(RepoService.class);
	}*/
}
