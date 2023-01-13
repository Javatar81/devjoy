package io.devjoy.operator.repository.gitea;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.WebApplicationException;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.jboss.resteasy.reactive.RestResponse.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.repository.Gitea;

@ApplicationScoped
@Gitea
public class UserServiceAdapter implements io.devjoy.operator.repository.domain.UserService{
	private static final Logger LOG = LoggerFactory.getLogger(UserServiceAdapter.class);
	@Override
	public List<String> getAllUsernames(String token, String baseUri) {
		try {
			UserService userService = getDynamicUrlClient(baseUri);
			return userService.getAll(token).stream().map(GetUserOutput::getUsername).toList();
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				return Collections.emptyList();
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
			return Collections.emptyList();
		}
		
	}

	@Override
	public void createUser(String username, String email, String token, String baseUri) {
		try {
			UserService userService = getDynamicUrlClient(baseUri);
			CreateUserInput input = new CreateUserInput();
			input.setEmail(email);
			input.setUsername(username);
			input.setPassword("devjoy");
			userService.create(token, input);
		} catch(WebApplicationException e) {
			if	(e.getResponse().getStatus() == StatusCode.NOT_FOUND) {
				LOG.error("Error calling repository API to create user", e);
			} else {
				throw e;
			}
		} catch (IllegalStateException | RestClientDefinitionException | URISyntaxException e) {
			LOG.error("Error calling repository API", e);
		}
	}
	
	private UserService getDynamicUrlClient(String baseUri) throws URISyntaxException {
		return  RestClientBuilder.newBuilder().baseUri(new URI(baseUri + "/api/v1")).build(UserService.class);
	}

}
