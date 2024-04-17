package io.devjoy.gitea.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.api.OrganizationApi;
import org.openapi.quarkus.gitea_json.model.APIError;
import org.openapi.quarkus.gitea_json.model.CreateOrgOption;
import org.openapi.quarkus.gitea_json.model.CreateOrgOption.VisibilityEnum;
import org.openapi.quarkus.gitea_json.model.EditOrgOption;
import org.openapi.quarkus.gitea_json.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationConditionType;
import io.devjoy.gitea.util.AuthorizationRequestFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class OrganizationService {
	private static final Logger LOG = LoggerFactory.getLogger(OrganizationService.class);
	
	private GiteaApiService apiService;
	
	public OrganizationService(GiteaApiService apiService) {
		this.apiService = apiService;
	}
	
	public List<Organization> getAllOrgs(Gitea gitea, String token) {
		List<Organization> noResult = Collections.emptyList();
		return apiService.getBaseUri(gitea).map(uri -> {
			LOG.info("getAllOrgs with uri={}", uri);
			try {
				return getDynamicUrlClient(new URIBuilder(uri).build(), AdminApi.class, token).adminGetAllOrgs(0, 1000);
			} catch (URISyntaxException e) {
				LOG.error("", e);
				throw new ServiceException("Repository cannot be found via API", e);
			}
		}).orElse(noResult);
	}
	
	public Optional<Organization> get(Gitea gitea, String orgName, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			LOG.info("Get org {} with uri={}", orgName, uri);
			try {
				return Optional.of(getDynamicUrlClient(new URIBuilder(uri).build(), OrganizationApi.class, token).orgGet(orgName));
			} catch (URISyntaxException e) {
				LOG.error("", e);
				throw new ServiceException("Repository cannot be found via API", e);
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404) {
					return Optional.empty();
				} else {
					throw new ServiceException(String.format("Org cannot be created via API error code=%s message=%s",
							e.getResponse().getStatus(), e.getResponse().readEntity(APIError.class).getMessage()), e, GiteaOrganizationConditionType.GITEA_API_ERROR);
				}
			}
		});
	}
	
	public void delete(Gitea gitea, String orgName, String token) {
		apiService.getBaseUri(gitea).ifPresent(uri -> {
			LOG.info("Delete org {} with uri={}", orgName, uri);
			try {
				getDynamicUrlClient(new URIBuilder(uri).build(), OrganizationApi.class, token).orgDelete(orgName);
			} catch (URISyntaxException e) {
				LOG.error("", e);
				throw new ServiceException("Repository cannot be found via API", e);
			}
		});
	}
	
	public Optional<Organization> create(Gitea gitea, String owner, String token, Organization org) {
		return apiService.getBaseUri(gitea).map(uri -> {
			
			CreateOrgOption createOrg = new CreateOrgOption();
			createOrg.setDescription(org.getDescription());
			createOrg.setEmail(org.getEmail());
			createOrg.setFullName(org.getFullName());
			createOrg.setLocation(org.getLocation());
			createOrg.setUsername(org.getUsername());
			createOrg.setRepoAdminChangeTeamAccess(false);
			createOrg.setVisibility(VisibilityEnum.valueOf(org.getVisibility()));
			createOrg.setWebsite(org.getWebsite());
			LOG.info("Creating org {}", createOrg);
			try {
				return getDynamicUrlClient(new URIBuilder(uri).build(), AdminApi.class, token).adminCreateOrg(owner, createOrg);
			} catch (URISyntaxException e) {
				throw new ServiceException("Org cannot be created via API", e, GiteaOrganizationConditionType.GITEA_API_ERROR);
			}	catch (WebApplicationException e) {
				throw new ServiceException(String.format("Org cannot be created via API error code=%s message=%s",
						e.getResponse().getStatus(), e.getResponse().readEntity(APIError.class).getMessage()), e, GiteaOrganizationConditionType.GITEA_API_ERROR);
			}
		});
	}
	
	public Optional<Organization> update(Gitea gitea, String orgName, String token, Organization org) {
	
		return apiService.getBaseUri(gitea).map(uri -> {
			
			EditOrgOption editOrg = new EditOrgOption();
			editOrg.setDescription(org.getDescription());
			editOrg.setEmail(org.getEmail());
			editOrg.setFullName(org.getFullName());
			editOrg.setLocation(org.getLocation());
			editOrg.setRepoAdminChangeTeamAccess(false);
			editOrg.setVisibility(EditOrgOption.VisibilityEnum.valueOf(org.getVisibility()));
			editOrg.setWebsite(org.getWebsite());
			LOG.info("Creating org {}", editOrg);
			try {
				return getDynamicUrlClient(new URIBuilder(uri).build(), OrganizationApi.class, token).orgEdit(orgName, editOrg);
			} catch (URISyntaxException e) {
				throw new ServiceException("Org cannot be updated via API", e, GiteaOrganizationConditionType.GITEA_API_ERROR);
			}	catch (WebApplicationException e) {
				throw new ServiceException(String.format("Org cannot be updated via API error code=%s message=%s",
						e.getResponse().getStatus(), e.getResponse().readEntity(APIError.class).getMessage()), e, GiteaOrganizationConditionType.GITEA_API_ERROR);
			}
		});
		
	}
	
	private <T> T getDynamicUrlClient(URI baseUri, Class<T> clazz, String token) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URIBuilder(baseUri).setPath("/api/v1").build())
				.register(AuthorizationRequestFilter.accessToken(token))
				.build(clazz);
	}
}
