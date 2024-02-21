package io.devjoy.gitea.domain.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.api.UserApi;
import org.openapi.quarkus.gitea_json.model.CreateUserOption;
import org.openapi.quarkus.gitea_json.model.EditUserOption;
import org.openapi.quarkus.gitea_json.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.TokenSupplierRequestFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class UserService {
	private static final String ERROR_IN_REST_CLIENT = "Error in rest client";

	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
	
	private GiteaApiService apiService;
	
	public UserService(GiteaApiService apiService) {
		this.apiService = apiService;
	}
	
	public Optional<User> getUser(Gitea gitea, String userName, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), UserApi.class, token).userGet(userName));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT, e);
				return Optional.empty();
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404) {
					return Optional.empty();
				} else {
					throw e;
				}
			} 
		});
	}
	
	public Optional<User> createUser(Gitea gitea, String userName, String token) {
		return createUser(gitea, userName, token, Optional.empty());
	}
	
	public Optional<User> createUser(Gitea gitea, String userName, String token, Optional<String> email) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				CreateUserOption createUser = new CreateUserOption();
				createUser.setEmail(email.orElse(userName + "@example.com"));
				createUser.setFullName(userName);
				createUser.setLoginName(userName);
				createUser.setUsername(userName);
				createUser.setMustChangePassword(true);
				createUser.setPassword("devjoy");
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), AdminApi.class, token).adminCreateUser(createUser));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT);
				return Optional.empty();
			}
		});
	}
	
	public Optional<User> changeUserPassword(Gitea gitea, String user, String password, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				EditUserOption editUser = new EditUserOption();
				editUser.setPassword(password);
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), AdminApi.class, token).adminEditUser(user, editUser));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT);
				return Optional.empty();
			}
		});
	}
	
	private <T> T getDynamicUrlClient(URI baseUri, Class<T> clazz, String token) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URIBuilder(baseUri).setPath("/api/v1").build())
				.register(new TokenSupplierRequestFilter(() -> token))
				.build(clazz);
	}
}
