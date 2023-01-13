package io.devjoy.operator.repository.gitea;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GetUserOutput {
	private String email;
	private String username;
	@JsonProperty("full_name")
	private String fullName;
	@JsonProperty("login_name")
	private String loginName;
	@JsonProperty("must_change_password")
	private boolean mustChangePassword;
	private boolean restricted;
	@JsonProperty("send_notify")
	private boolean sendNotify;
	private String visibility;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getFullName() {
		return fullName;
	}
	public void setFullName(String fullName) {
		this.fullName = fullName;
	}
	public String getLoginName() {
		return loginName;
	}
	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}
	public boolean isMustChangePassword() {
		return mustChangePassword;
	}
	public void setMustChangePassword(boolean mustChangePassword) {
		this.mustChangePassword = mustChangePassword;
	}
	public boolean isRestricted() {
		return restricted;
	}
	public void setRestricted(boolean restricted) {
		this.restricted = restricted;
	}
	public boolean isSendNotify() {
		return sendNotify;
	}
	public void setSendNotify(boolean sendNotify) {
		this.sendNotify = sendNotify;
	}
	public String getVisibility() {
		return visibility;
	}
	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}
}
