package io.devjoy.gitea.api;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.openapi.quarkus.gitea_json.model.AccessToken;
import org.openapi.quarkus.gitea_json.model.CreateAccessTokenOption;

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
