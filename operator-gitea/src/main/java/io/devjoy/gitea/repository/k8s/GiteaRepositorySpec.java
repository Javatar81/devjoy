package io.devjoy.gitea.repository.k8s;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.devjoy.gitea.repository.domain.Visibility;

public class GiteaRepositorySpec {

	@JsonPropertyDescription("The visibility of the repository: PRIVATE (default) or PUBLIC.")
	private Visibility visibility = Visibility.PRIVATE;
	@JsonPropertyDescription("The user owning the repository.")
	@JsonProperty(required = true)
	private String user;
	@JsonPropertyDescription("Whether the repository should be deleted with the repository resource. Default is true.")
	@JsonProperty(defaultValue = "true")
	private boolean deleteOnFinalize = true;

	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public Visibility getVisibility() {
		return visibility;
	}
	public void setVisibility(Visibility visibility) {
		this.visibility = visibility;
	}
	public boolean isDeleteOnFinalize() {
		return deleteOnFinalize;
	}
	public void setDeleteOnFinalize(boolean deleteRepoOnFinalize) {
		this.deleteOnFinalize = deleteRepoOnFinalize;
	}
}
