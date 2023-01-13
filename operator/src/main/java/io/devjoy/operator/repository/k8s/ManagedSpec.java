package io.devjoy.operator.repository.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.devjoy.operator.repository.domain.GitProvider;
import io.devjoy.operator.repository.domain.Visibility;

public class ManagedSpec {
	@JsonPropertyDescription("The visibility of the repository: PRIVATE (default) or PUBLIC.")
	private Visibility visibility = Visibility.PRIVATE;
	@JsonPropertyDescription("The user owning the repository.")
	private String user;
	@JsonPropertyDescription("The Git provider: GITEA (default) or GITHUB.")
	private GitProvider provider = GitProvider.GITEA;
	@JsonPropertyDescription("Whether the repository should be deleted with the repository resource.")
	private boolean deleteRepoOnFinalize = true;

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
	public GitProvider getProvider() {
		return provider;
	}
	public void setProvider(GitProvider provider) {
		this.provider = provider;
	}
	public boolean isDeleteRepoOnFinalize() {
		return deleteRepoOnFinalize;
	}
	public void setDeleteRepoOnFinalize(boolean deleteRepoOnFinalize) {
		this.deleteRepoOnFinalize = deleteRepoOnFinalize;
	}
}
