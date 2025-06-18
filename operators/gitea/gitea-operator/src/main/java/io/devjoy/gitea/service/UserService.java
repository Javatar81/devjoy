package io.devjoy.gitea.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.http.client.utils.URIBuilder;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.api.UserApi;
import org.openapi.quarkus.gitea_json.model.AccessToken;
import org.openapi.quarkus.gitea_json.model.CreateAccessTokenOption;
import org.openapi.quarkus.gitea_json.model.CreateUserOption;
import org.openapi.quarkus.gitea_json.model.EditUserOption;
import org.openapi.quarkus.gitea_json.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.AuthorizationRequestFilter;
import io.netty.util.internal.StringUtil;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class UserService {
	private static final String ERROR_IN_REST_CLIENT = "Error in rest client";
	public static final String SCOPE_WRITE_REPO = "write:repository";
	public static final String SCOPE_WRITE_USER = "write:user";
	/**
	 * We need Gitea 1.20+ to use this scope
	 */
	private static final String SCOPE_WRITE_ADMIN = "write:admin";
	private static final String SCOPE_WRITE_ACTIVITYPUB = "write:activitypub";
	private static final String SCOPE_WRITE_ISSUE = "write:issue";
	private static final String SCOPE_WRITE_MISC = "write:misc";
	private static final String SCOPE_WRITE_NOTIFICATION = "write:notification";
	private static final String SCOPE_WRITE_ORGANIZATION = "write:organization";
	private static final String SCOPE_WRITE_PACKAGE = "write:package";

	private static final Logger LOG = LoggerFactory.getLogger(UserService.class);
	
	private GiteaApiService apiService;
	
	public UserService(GiteaApiService apiService) {
		this.apiService = apiService;
	}
	
	public Optional<User> getUser(Gitea gitea, String userName, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			LOG.debug("Get user {} with uri={}", userName, uri);
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
	
	public List<User> getAllUsers(Gitea gitea, String token) {
		List<User> noResult = Collections.emptyList();
		return apiService.getBaseUri(gitea).map(uri -> {
			try {
				return getDynamicUrlClient(new URI(uri), AdminApi.class, token).adminSearchUsers(null, null, 0, 1000);
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT, e);
				return noResult;
			} catch (WebApplicationException e) {
				if (e.getResponse().getStatus() == 404) {
					return noResult;
				} else {
					throw e;
				}
			} 
		}).orElse(noResult);
	}
	
	public Optional<User> createUser(Gitea gitea, String userName, String token) {
		return createUser(gitea, userName, token, Optional.empty());
	}
	
	
	public Optional<User> createUser(Gitea gitea, String userName, String token, Optional<String> email) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {

			User user = new User();
			user.setEmail(email.orElse(userName + "@example.com"));
			user.setFullName(userName);
			user.setLoginName(userName);
			return createUser(gitea, user, token);
		});
	}
	
	public Optional<User> createUser(Gitea gitea, User user, String token) {
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				LOG.debug("Create user {} with uri={}", user.getLogin(), uri);
				CreateUserOption createUser = new CreateUserOption();
				createUser.setEmail(user.getEmail());
				createUser.setFullName(user.getFullName());
				createUser.setLoginName(user.getLoginName());
				createUser.setUsername(user.getLoginName());
				createUser.setMustChangePassword(true);
				createUser.setPassword("devjoypw");
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), AdminApi.class, token).adminCreateUser(createUser));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT);
				return Optional.empty();
			}
		});
	}
	
	public Optional<User> changeUserPassword(Gitea gitea, String user, String password, String token) {
		LOG.info("Changing password for {} via API. Token set: {}, Pw set: {}", user, !StringUtil.isNullOrEmpty(token), !StringUtil.isNullOrEmpty(password));
		if (password!= null && password.length() < 8) {
			LOG.warn("Password length < 8. This might not be accepted by Gitea.");
		}
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				EditUserOption editUser = new EditUserOption();
				editUser.setLoginName(user);
				editUser.setPassword(password);
				return Optional.ofNullable(getDynamicUrlClient(new URI(uri), AdminApi.class, token).adminEditUser(user, editUser));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT);
				return Optional.empty();
			}
		});
	}
	
	
	
	public Optional<AccessToken> createAdminAccessToken(Gitea gitea, String userName, String password, String tokenName) {
		return createAccessToken(gitea, userName, password, tokenName, SCOPE_WRITE_REPO, SCOPE_WRITE_USER, SCOPE_WRITE_ADMIN,SCOPE_WRITE_ORGANIZATION);
	}
	
	public Optional<AccessToken> createAccessToken(Gitea gitea, String userName, String password, String tokenName, String ... scopes) {
		LOG.debug("Creating token {} for user {} with scopes {}", tokenName, userName, Arrays.asList(scopes));
		return apiService.getBaseUri(gitea).flatMap(uri -> {
			try {
				CreateAccessTokenOption createToken = new CreateAccessTokenOption();
				createToken.setName(tokenName);
				createToken.setScopes(Arrays.asList(scopes));
				Optional<AccessToken> token = Optional.ofNullable(getDynamicUrlClient(new URI(uri), UserApi.class, userName, password)
						.userCreateToken(userName, createToken));
				LOG.debug("Token available: {}", token.isPresent());
				return token;
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT, e);
				return Optional.empty();
			}
		});
	}

	public boolean hasAccessToken(Gitea gitea, String userName, String password, String tokenName) {
		return apiService.getBaseUri(gitea).map(uri -> {
			try {
				List<AccessToken> tokens = getDynamicUrlClient(new URI(uri), UserApi.class, userName, password)
						.userGetTokens(userName, 0, 1000);
				return tokens.stream()
					.anyMatch(t -> t.getName().equals(tokenName));
			} catch (URISyntaxException e) {
				LOG.error(ERROR_IN_REST_CLIENT, e);
				return false;
			}
		}).orElse(false);
	}
	
	private <T> T getDynamicUrlClient(URI baseUri, Class<T> clazz, String token) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URIBuilder(baseUri).setPath("/api/v1").build())
				.register(AuthorizationRequestFilter.accessToken(token))
				.build(clazz);
	}
	
	private <T> T getDynamicUrlClient(URI baseUri, Class<T> clazz, String userName, String password) throws URISyntaxException {
		return RestClientBuilder.newBuilder()
				.baseUri(new URIBuilder(baseUri).setPath("/api/v1").build())
				.register(AuthorizationRequestFilter.basicAuth(userName, password))
				.build(clazz);
	}
}
