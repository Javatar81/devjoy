package io.devjoy.operator.repository.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class RepositorySpec {
	@JsonPropertyDescription("The Git provider: GITEA (default) or GITHUB")
	private ManagedSpec managed;
	@JsonPropertyDescription("The Url for the git repository when not created by this resource")
	private String existingRepositoryCloneUrl;

	public ManagedSpec getManaged() {
		return managed;
	}

	public void setManaged(ManagedSpec managed) {
		this.managed = managed;
	}

	public String getExistingRepositoryCloneUrl() {
		return existingRepositoryCloneUrl;
	}

	public void setExistingRepositoryCloneUrl(String existingRepositoryCloneUrl) {
		this.existingRepositoryCloneUrl = existingRepositoryCloneUrl;
	}
	
}
