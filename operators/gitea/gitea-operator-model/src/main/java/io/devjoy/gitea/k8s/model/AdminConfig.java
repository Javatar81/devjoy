package io.devjoy.gitea.k8s.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class AdminConfig {
    @JsonPropertyDescription("The name of the admin user. Must not be 'admin' because it is reserved.")
	@JsonProperty(defaultValue = "devjoyadmin")
	private String adminUser = "devjoyadmin";
	@JsonPropertyDescription("The email of the admin user")
	@JsonProperty(defaultValue = "admin@example.com")
	private String adminEmail = "admin@example.com";
	@JsonPropertyDescription("The optional admin password. If not set it will be generated. Once set the value will be moved to a secret.")
	private String adminPassword;
	@JsonPropertyDescription("The length of the generated admin password. Value is ignored if adminPassword is set. Min length is 10.")
	private int adminPasswordLength = 10; 
	@JsonPropertyDescription("The optional secret containing the admin password.")
	private String extraAdminSecretName;
	@JsonPropertyDescription("The optional secret containing the admin password.")
	@JsonProperty(defaultValue = "password")
	private String extraAdminSecretPasswordKey = "password";

    public String getAdminUser() {
		return adminUser;
	}
	public void setAdminUser(String adminUser) {
		this.adminUser = adminUser;
	}
	public String getAdminEmail() {
		return adminEmail;
	}
	public void setAdminEmail(String adminEmail) {
		this.adminEmail = adminEmail;
	}
	public int getAdminPasswordLength() {
		return adminPasswordLength;
	}
	public void setAdminPasswordLength(int adminPasswordLength) {
		this.adminPasswordLength = adminPasswordLength;
	}
	public String getAdminPassword() {
		return adminPassword;
	}
	public void setAdminPassword(String giteaAdminPassword) {
		this.adminPassword = giteaAdminPassword;
	}
    public String getExtraAdminSecretName() {
		return extraAdminSecretName;
	}
	public void setExtraAdminSecretName(String extraAdminSecretName) {
		this.extraAdminSecretName = extraAdminSecretName;
	}
	public String getExtraAdminSecretPasswordKey() {
		return extraAdminSecretPasswordKey;
	}
	public void setExtraAdminSecretPasswordKey(String extraAdminSecretPasswordKey) {
		this.extraAdminSecretPasswordKey = extraAdminSecretPasswordKey;
	}
}
