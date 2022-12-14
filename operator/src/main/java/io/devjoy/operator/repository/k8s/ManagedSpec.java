package io.devjoy.operator.repository.k8s;

import io.devjoy.operator.repository.domain.GitProvider;
import io.devjoy.operator.repository.domain.Visibility;

public class ManagedSpec {

	private Visibility visibility = Visibility.PRIVATE;
	private String user;
	private GitProvider provider = GitProvider.GITEA;
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
