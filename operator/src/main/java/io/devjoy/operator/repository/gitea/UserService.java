package io.devjoy.operator.repository.gitea;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
public interface UserService {
	@POST
	@Path("/admin/users")
	GetUserOutput create(@HeaderParam("Authorization") String authorization, CreateUserInput body);
	
	@GET
	@Path("/admin/users")
	List<GetUserOutput> getAll(@HeaderParam("Authorization") String authorization);
}
