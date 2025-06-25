package io.devjoy.gitea.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.api.MiscellaneousApi;
import org.openapi.quarkus.gitea_json.model.ServerVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.AuthorizationRequestFilter;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MiscService {
    private static final Logger LOG = LoggerFactory.getLogger(MiscService.class);
	private static final String ERROR_IN_REST_CLIENT = "Error in rest client";
	private GiteaApiService apiService;
	
	public MiscService(GiteaApiService apiService) {
		this.apiService = apiService;
	}

    public Optional<ServerVersion> getVersion(Gitea gitea, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
            var userName = gitea.getSpec().getAdminConfig().getAdminUser();
			LOG.debug("Get user {} with uri={}", userName, uri);
			try {
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), MiscellaneousApi.class, token).getVersion());
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT, e);
				return Optional.empty();
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
