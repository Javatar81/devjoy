package io.devjoy.operator.environment.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class GiteaConfigSpec {
	@JsonPropertyDescription("Whether Gitea is used as the Git provider.")
	private boolean enabled = true;
	@JsonPropertyDescription("Whether Gitea is managed via this resource.")
	private boolean managed = true;
	@JsonPropertyDescription("The name of the Gitea resource in the same namespace.")
	private String resourceName;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isManaged() {
		return managed;
	}
	public void setManaged(boolean managed) {
		this.managed = managed;
	}
	public String getResourceName() {
		return resourceName;
	}
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
}
