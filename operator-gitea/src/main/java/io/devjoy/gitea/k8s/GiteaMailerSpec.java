package io.devjoy.gitea.k8s;

public class GiteaMailerSpec {
	private boolean enabled;
	private String from;
	private String type;
	private String host;
	private String user;
	private String password;
	private String heloHostname;
	private boolean registerEmailConfirm;
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
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
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
	public boolean isRegisterEmailConfirm() {
		return registerEmailConfirm;
	}
	public void setRegisterEmailConfirm(boolean registerEmailConfirm) {
		this.registerEmailConfirm = registerEmailConfirm;
	}
	public boolean isEnableNotifyMail() {
		return enableNotifyMail;
	}
	public void setEnableNotifyMail(boolean enableNotifyMail) {
		this.enableNotifyMail = enableNotifyMail;
	}
	
	
}
