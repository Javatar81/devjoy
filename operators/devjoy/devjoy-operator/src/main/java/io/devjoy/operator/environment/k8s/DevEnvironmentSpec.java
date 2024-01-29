package io.devjoy.operator.environment.k8s;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class DevEnvironmentSpec {
	@JsonPropertyDescription("The Gitea configuration.")
    private GiteaConfigSpec gitea;
	@JsonPropertyDescription("The name of an existing PVC to store the maven settings and cache the maven repository.")
	private String mavenSettingsPvc;
	
	public GiteaConfigSpec getGitea() {
		return gitea;
	}
	public void setGitea(GiteaConfigSpec gitea) {
		this.gitea = gitea;
	}
	public String getMavenSettingsPvc() {
		return mavenSettingsPvc;
	}
	public void setMavenSettingsPvc(String mavenSettingsPvc) {
		this.mavenSettingsPvc = mavenSettingsPvc;
	} 

}
