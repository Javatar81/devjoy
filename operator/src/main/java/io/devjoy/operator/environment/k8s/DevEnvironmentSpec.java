package io.devjoy.operator.environment.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class DevEnvironmentSpec {
	@JsonPropertyDescription("The Gitea configuration.")
    private GiteaConfigSpec gitea;
	
	public GiteaConfigSpec getGitea() {
		return gitea;
	}

	public void setGitea(GiteaConfigSpec gitea) {
		this.gitea = gitea;
	}

}
