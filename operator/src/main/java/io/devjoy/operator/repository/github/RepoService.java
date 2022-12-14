package io.devjoy.operator.repository.github;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("")
@RegisterRestClient
public interface RepoService {
	  @POST
	  @Path("/user/repos")
	  GetRepoOutput create(@HeaderParam("Authorization") String authorization, CreateRepoInput body);
	  
	  @GET
	  @Path("/repos/{owner}/{repo}")
	  GetRepoOutput getByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner, @PathParam("repo") String repo);

	  @DELETE
	  @Path("/repos/{owner}/{repo}")
	  GetRepoOutput deleteByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner, @PathParam("repo") String repo);
}
