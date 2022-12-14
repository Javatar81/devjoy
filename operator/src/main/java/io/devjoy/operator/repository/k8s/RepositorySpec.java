package io.devjoy.operator.repository.k8s;

public class RepositorySpec {

	private ManagedSpec managed;
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
