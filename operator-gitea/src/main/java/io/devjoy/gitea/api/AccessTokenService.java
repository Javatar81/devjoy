package io.devjoy.gitea.api;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.gitea_json.model.AccessToken;
import org.openapi.quarkus.gitea_json.model.CreateAccessTokenOption;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("")
@RegisterRestClient
public interface AccessTokenService {
	@POST
	@Path("/users/{username}/tokens")
	AccessToken userCreateToken(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username, CreateAccessTokenOption body);
	
	@DELETE
	@Path("/users/{username}/tokens/{token}")
	void userDeleteToken(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username, @PathParam("token") String tokenName);
	
	@GET
	@Path("/users/{username}/tokens")
	List<AccessToken> userGetTokens(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username);
}
