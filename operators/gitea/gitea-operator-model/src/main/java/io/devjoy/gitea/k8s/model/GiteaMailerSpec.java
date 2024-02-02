package io.devjoy.gitea.k8s.model;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class GiteaMailerSpec {
	@JsonPropertyDescription("Enable to use a mail service.")
	private boolean enabled;
	@JsonPropertyDescription("Mail from address, RFC 5322. This can be just an email address, or the \"Name\" \\email@example.com\\ format.")
	private String from;
	@JsonPropertyDescription("Mail server protocol. One of \"smtp\", \"smtps\", \"smtp+starttls\", \"smtp+unix\", \"sendmail\", \"dummy\"")
	private String protocol;
	@JsonPropertyDescription("Mail server address + port. e.g. smtp.gmail.com. For smtp+unix, this should be a path to a unix socket instead. Mail server port. If no protocol is specified, it will be inferred by this setting")
	private String host;
	@JsonPropertyDescription("Username of mailing user (usually the sender's e-mail address).")
	private String user;
	@JsonPropertyDescription("Password of mailing user. Use `your password` for quoting if you use special characters in the password.")
	private String password;
	@JsonPropertyDescription("HELO hostname. If empty it is retrieved from system.")
	private String heloHostname;
	@JsonPropertyDescription("Enable this to ask for mail confirmation of registration")
	private boolean enableNotifyMail;
	
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public String getFrom() {
		return from;
	}
	public void setFrom(String from) {
		this.from = from;
	}
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getHeloHostname() {
		return heloHostname;
	}
	public void setHeloHostname(String heloHostname) {
		this.heloHostname = heloHostname;
	}
	public boolean isEnableNotifyMail() {
		return enableNotifyMail;
	}
	public void setEnableNotifyMail(boolean enableNotifyMail) {
		this.enableNotifyMail = enableNotifyMail;
	}
	
	
}
