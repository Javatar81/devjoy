package io.devjoy.operator.project.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ProjectSpec {
	@JsonPropertyDescription("The clone Url of your existing Git that is not managed by the project.")
	private String existingRepositoryCloneUrl;
	@JsonPropertyDescription("The reference to the environment namespace.")
	private String environmentNamespace;
	@JsonPropertyDescription("The reference to the environment name.")
	private String environmentName;
	@JsonPropertyDescription("The owner of the project. Can be a user or an organization.")
	private ProjectOwner owner;
	@JsonPropertyDescription("The quarkus configuration if this is a quarkus project.")
	private QuarkusSpec quarkus;
	
	public String getExistingRepositoryCloneUrl() {
		return existingRepositoryCloneUrl;
	}
	public void setExistingRepositoryCloneUrl(String existingRepositoryCloneUrl) {
		this.existingRepositoryCloneUrl = existingRepositoryCloneUrl;
	}
	public String getEnvironmentNamespace() {
		return environmentNamespace;
	}
	public void setEnvironmentNamespace(String environmentNamespace) {
		this.environmentNamespace = environmentNamespace;
	}
	public String getEnvironmentName() {
		return environmentName;
	}
	public void setEnvironmentName(String environmentName) {
		this.environmentName = environmentName;
	}
	public ProjectOwner getOwner() {
		return owner;
	}
	public void setOwner(ProjectOwner owner) {
		this.owner = owner;
	}
	public QuarkusSpec getQuarkus() {
		return quarkus;
	}
	public void setQuarkus(QuarkusSpec quarkus) {
		this.quarkus = quarkus;
	} 
}
