package io.devjoy.operator.project.k8s;

public class RepositoryStatus {
	private boolean userSecretAvailable = false;

	public boolean isUserSecretAvailable() {
		return userSecretAvailable;
	}
	public void setUserSecretAvailable(boolean userSecretAvailable) {
		this.userSecretAvailable = userSecretAvailable;
	}
}
