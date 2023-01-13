package io.devjoy.operator.environment.gitea;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.repository.domain.Token;
import io.fabric8.kubernetes.client.utils.Base64;

@ApplicationScoped
public class TokenServiceAdapter implements io.devjoy.operator.environment.k8s.domain.TokenService{
	
	private static final String BASIC_AUTH_PREFIX = "Basic ";
	private static final Logger LOG = LoggerFactory.getLogger(TokenServiceAdapter.class);
	
	@Override
	public List<Token> getTokensByUser(String user, String password, String baseUri) {
		try {
			AccessTokenService tokenService = getDynamicUrlClient(baseUri);
			return tokenService.getAll(BASIC_AUTH_PREFIX + Base64.encodeBytes((user + ":" + password).getBytes()), user)
					.stream()
					.map(AccessTokenOutput::toToken)
					.collect(Collectors.toList());
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				return Collections.emptyList();
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API to get user tokens", e);
			return Collections.emptyList();
		}
	}
	
	@Override
	public Optional<Token> createTokenForUser(String user, String password, String tokenName, String baseUri) {
		try {
			AccessTokenService tokenService = getDynamicUrlClient(baseUri);
			CreateAccessTokenInput input = new CreateAccessTokenInput();
			input.setName(tokenName);
			return Optional.ofNullable(tokenService.create(BASIC_AUTH_PREFIX + Base64.encodeBytes((user + ":" + password).getBytes()), user, input))
					.map(AccessTokenOutput::toToken);
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				return Optional.empty();
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API to create user tokens", e);
			return Optional.empty();
		}
	}
	
	@Override
	public void deleteTokenForUser(String user, String password, String tokenName, String baseUri) {
		try {
			AccessTokenService tokenService = getDynamicUrlClient(baseUri);
			CreateAccessTokenInput input = new CreateAccessTokenInput();
			input.setName(tokenName);
			tokenService.delete(BASIC_AUTH_PREFIX + Base64.encodeBytes((user + ":" + password).getBytes()), user, tokenName);
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				LOG.warn(String.format("Token %s not found. Skipping delete", tokenName), e);
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API to delete user token", e);
		}
	}
	
	private AccessTokenService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(AccessTokenService.class);
	}

}
