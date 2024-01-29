package io.devjoy.operator.project.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class ProjectOwner {
	@JsonPropertyDescription("The username of the owning user. Can be empty when organization is specified")
	private String user;
	@JsonPropertyDescription("The user email of the owning user. If empty one will be generated.")
	private String userEmail;
	@JsonPropertyDescription("The organization owning the project. If left empty a user must be set.")
	private String organization;
	
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getOrganization() {
		return organization;
	}
	public void setOrganization(String organization) {
		this.organization = organization;
	}
	public String getUserEmail() {
		return userEmail;
	}
	public void setUserEmail(String userEmail) {
		this.userEmail = userEmail;
	}
}
