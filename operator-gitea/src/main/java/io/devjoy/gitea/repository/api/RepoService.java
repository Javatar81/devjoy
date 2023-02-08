package io.devjoy.gitea.repository.api;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.Repository;

@Path("")
@RegisterRestClient
public interface RepoService {
	  @POST
	  @Path("/user/repos")
	  Repository create(@HeaderParam("Authorization") String authorization, CreateRepoOption body);
	  
	  @GET
	  @Path("/repos/{owner}/{repo}")
	  Repository getByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner, @PathParam("repo") String repo);

	  @DELETE
	  @Path("/repos/{owner}/{repo}")
	  Repository deleteByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner, @PathParam("repo") String repo);
}