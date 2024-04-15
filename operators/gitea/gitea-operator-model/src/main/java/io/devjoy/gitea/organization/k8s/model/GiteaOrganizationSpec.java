package io.devjoy.gitea.organization.k8s.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.devjoy.gitea.organization.domain.OrganizationVisibility;

public class GiteaOrganizationSpec {
	@JsonPropertyDescription("The user who owns the org.")
	private String owner;
	@JsonPropertyDescription("The visibility of the org. One of private, limited, public.")
	@JsonProperty(defaultValue = "private")
	private OrganizationVisibility visibility = OrganizationVisibility.PRIVATE;
	@JsonPropertyDescription("The description of the org.")
	private String description;
	@JsonPropertyDescription("The email of the owner.")
	private String ownerEmail;
	@JsonPropertyDescription("The location of the org.")
	private String location;
	@JsonPropertyDescription("The website of the org.")
	private String website;
	
	
	public String getOwner() {
		return owner;
	}
	public void setOwner(String owner) {
		this.owner = owner;
	}
	public OrganizationVisibility getVisibility() {
		return visibility;
	}
	public void setVisibility(OrganizationVisibility visibility) {
		this.visibility = visibility;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getOwnerEmail() {
		return ownerEmail;
	}
	public void setOwnerEmail(String email) {
		this.ownerEmail = email;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	
}
