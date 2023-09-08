package io.devjoy.gitea.repository.domain;

import static io.devjoy.gitea.repository.domain.Visibility.PRIVATE;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.openapi.quarkus.gitea_json.model.CreateHookOption;
import org.openapi.quarkus.gitea_json.model.CreateHookOption.TypeEnum;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.Hook;
import org.openapi.quarkus.gitea_json.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ServiceException;
import io.devjoy.gitea.repository.api.RepoService;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaRepositoryConditionType;
import io.devjoy.gitea.repository.k8s.SecretReferenceSpec;
import io.devjoy.gitea.repository.k8s.WebhookSpec;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;


@ApplicationScoped
public class RepositoryService {
	private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

	public void createWebHooks(GiteaRepository repository, Map<SecretReferenceSpec, String> secrets, String token, String baseUri) {
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
	}
	
	private boolean hookEquals(Hook a, WebhookSpec b) {
		return a.getType().equalsIgnoreCase(b.getType()) 
				&& Objects.equals(a.getConfig().get("url"), b.getTargetUrl()) 
				&& Objects.equals(a.getEvents(), b.getEvents());
	}
		
	public Repository createIfNotExists(GiteaRepository repository, String token, String baseUri) {
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

	public void delete(GiteaRepository repository, String token) {
		try {
			URL cloneUrl = new URL(repository.getStatus().getCloneUrl());
			String port = cloneUrl.getPort() > -1 ? ":" + cloneUrl.getPort() : ":";
			String baseUri = String.format("%s://%s%s", cloneUrl.getProtocol(), cloneUrl.getHost(), port);
			RepoService repoService = getDynamicUrlClient(baseUri);
			repoService.deleteByUserAndName(token, repository.getSpec().getUser(), repository.getMetadata().getName());
		} catch (URISyntaxException | MalformedURLException e) {
			LOG.error("Error calling repository API to delete user", e);
		}
		
	}
	
	private RepoService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(RepoService.class);
	}
}
