package io.devjoy.operator.environment.gitea;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
public interface AccessTokenService {
	@POST
	@Path("/users/{username}/tokens")
	AccessTokenOutput create(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username, CreateAccessTokenInput body);
	
	@DELETE
	@Path("/users/{username}/tokens/{token}")
	AccessTokenOutput delete(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username, @PathParam("token") String tokenName);
	
	@GET
	@Path("/users/{username}/tokens")
	List<AccessTokenOutput> getAll(@HeaderParam("Authorization") String authorization,
			@PathParam("username") String username);
}
