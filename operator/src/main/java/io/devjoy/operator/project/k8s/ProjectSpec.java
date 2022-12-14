package io.devjoy.operator.project.k8s;

public class ProjectSpec {
	//https://javaoperatorsdk.io/docs/dependent-resources
	private String existingRepositoryCloneUrl;
	
	public String getExistingRepositoryCloneUrl() {
		return existingRepositoryCloneUrl;
	}

	public void setExistingRepositoryCloneUrl(String existingRepositoryCloneUrl) {
		this.existingRepositoryCloneUrl = existingRepositoryCloneUrl;
	}
	
	
    
}
