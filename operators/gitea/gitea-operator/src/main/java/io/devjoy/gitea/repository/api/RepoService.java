package io.devjoy.gitea.repository.api;

import java.util.List;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.annotations.jaxrs.HeaderParam;
import org.openapi.quarkus.gitea_json.model.CreateHookOption;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.Hook;
import org.openapi.quarkus.gitea_json.model.Repository;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("")
@RegisterRestClient
public interface RepoService {
	@POST
	@Path("/user/repos")
	Repository create(@HeaderParam("Authorization") String authorization, CreateRepoOption body);

	@GET
	@Path("/repos/{owner}/{repo}")
	Repository getByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner,
			@PathParam("repo") String repo);

	@DELETE
	@Path("/repos/{owner}/{repo}")
	Repository deleteByUserAndName(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner,
			@PathParam("repo") String repo);

	@POST
	@Path("/repos/{owner}/{repo}/hooks")
	public Hook createHook(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner,
			@PathParam("repo") String repo, CreateHookOption body);
	
	@GET
	@Path("/repos/{owner}/{repo}/hooks")
	public List<Hook> getHooks(@HeaderParam("Authorization") String authorization, @PathParam("owner") String owner,
			@PathParam("repo") String repo);
}