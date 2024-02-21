package io.devjoy.gitea.domain;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.openapi.quarkus.gitea_json.model.AccessToken;
import org.openapi.quarkus.gitea_json.model.CreateAccessTokenOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.api.AccessTokenService;
import io.devjoy.gitea.domain.service.GiteaPodExecService;
import io.devjoy.gitea.domain.service.ServiceException;
import io.devjoy.gitea.k8s.model.Gitea;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TokenService {
	private static final Logger LOG = LoggerFactory.getLogger(TokenService.class);
	private static final String TOKEN_NAME = "devjoy";
	/** See https://docs.gitea.com/development/oauth2-provider#scopes **/
	private static final String SCOPE_WRITE_REPO = "write:repository";
	private static final String SCOPE_WRITE_USER = "write:user";
	private static final String SCOPE_WRITE_ADMIN = "write:admin";
	private static final String SCOPE_WRITE_ACTIVITYPUB = "write:activitypub";
	private static final String SCOPE_WRITE_ISSUE = "write:issue";
	private static final String SCOPE_WRITE_MISC = "write:misc";
	private static final String SCOPE_WRITE_NOTIFICATION = "write:notification";
	private static final String SCOPE_WRITE_ORGANIZATION = "write:organization";
	private static final String SCOPE_WRITE_PACKAGE = "write:package";

	private final GiteaPodExecService execService;
	
	public TokenService(GiteaPodExecService execService) {
		super();
		this.execService = execService;
	}

	private Optional<String> createUserTokenViaCli(Gitea gitea, String userName, String tokenName, String ... scopes) {
		// We cannot delete nor create a unique token
		Command cmd = Command.builder()
				.withExecutable("/usr/bin/giteacmd")
				.withArgs(List.of("admin", "user", "generate-access-token"))
				.addOption(new Option("username", userName))
				.addOption(new Option("token-name", tokenName))
				.addOption(new Option("scopes", Arrays.stream(scopes).collect(Collectors.joining(","))))
				.build();
		Pattern pattern = Pattern.compile("created:\\s+([a-f0-9_\\-]+)");
		Optional<String> execResult = execService.execOnDeployment(gitea, cmd);
		execResult.ifPresent(r -> LOG.info("Exec result is {}", r));
		return execResult.map(pattern::matcher)
				.filter(Matcher::find)
				.map(m -> m.group(1));
	}
	
	public Optional<String> createUserTokenViaCli(Gitea gitea, String userName, String tokenName) {
		return createUserTokenViaCli(gitea, userName, tokenName, SCOPE_WRITE_REPO, SCOPE_WRITE_USER);
	}
	
	public Optional<String> createAdminTokenViaCli(Gitea gitea, String userName, String tokenName) {
		return createUserTokenViaCli(gitea, userName, tokenName, SCOPE_WRITE_ADMIN, SCOPE_WRITE_REPO, 
				SCOPE_WRITE_USER, SCOPE_WRITE_ACTIVITYPUB, SCOPE_WRITE_ISSUE, SCOPE_WRITE_MISC, 
				SCOPE_WRITE_NOTIFICATION, SCOPE_WRITE_ORGANIZATION, SCOPE_WRITE_PACKAGE);
	}
	
	public AccessToken replaceUserToken(String baseUri, String userName, String password) {
		try {
			LOG.info("Replacing token for user {} calling {}", userName, baseUri);
			return getDynamicUrlClient(baseUri, AccessTokenService.class).userGetTokens(getTokenFrom(userName, password), userName).stream()
					.filter(t -> TOKEN_NAME.equals(t.getName()))
					.findFirst()
					.map(t -> {
						LOG.info("Deleting devjoy token for user {} ", userName);
						deleteUserToken(baseUri, userName, password);
						LOG.info("Creating new devjoy token for user {} ", userName);
						return createUserToken(baseUri, userName, password);
					})
					.orElseGet(() -> {
						LOG.info("devjoy token does not exist for user {}. Creating it.", userName);
						return createUserToken(baseUri, userName, password);
					});
		} catch (URISyntaxException e) {
			throw new ServiceException("Error replacing user token via api", e);
		}		
	}
	
	public AccessToken createUserToken(String baseUri, String userName, String password) {
		try {
			CreateAccessTokenOption userCreateToken = new CreateAccessTokenOption();
			userCreateToken.setName(TOKEN_NAME);
			return getDynamicUrlClient(baseUri, AccessTokenService.class).userCreateToken(getTokenFrom(userName, password), userName, userCreateToken);
		} catch (URISyntaxException e) {
			throw new ServiceException("Error creating user token via api", e);
		}
	}
	
	public void deleteUserToken(String baseUri, String userName, String password) {
		try {
			CreateAccessTokenOption userCreateToken = new CreateAccessTokenOption();
			userCreateToken.setName(TOKEN_NAME);
			getDynamicUrlClient(baseUri, AccessTokenService.class).userDeleteToken(getTokenFrom(userName, password), userName, TOKEN_NAME);
		} catch (URISyntaxException e) {
			throw new ServiceException("Error creating user token via api", e);
		}
	}

	private <T> T getDynamicUrlClient(String baseUri, Class<T> clazz) throws URISyntaxException {
		return RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(clazz);
	}
	
	private String getTokenFrom(String username, String password) {
		return String.format("Basic %s", Base64.getEncoder().encode((username +  ":" + password).getBytes()));
	}
}
