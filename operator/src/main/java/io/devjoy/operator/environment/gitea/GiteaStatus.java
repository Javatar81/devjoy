package io.devjoy.operator.environment.gitea;

import java.util.List;

import io.fabric8.kubernetes.api.model.Condition;

public class GiteaStatus {
	private String adminPassword;
	private boolean adminSetupComplete;
	private List<Condition> conditions;
	private String giteaHostname;
	private String giteaRoute;
	private String userPassword;
	private boolean  userSetupComplete;
	
	public String getAdminPassword() {
		return adminPassword;
	}
	public void setAdminPassword(String adminPassword) {
		this.adminPassword = adminPassword;
	}
	public boolean isAdminSetupComplete() {
		return adminSetupComplete;
	}
	public void setAdminSetupComplete(boolean adminSetupComplete) {
		this.adminSetupComplete = adminSetupComplete;
	}
	public List<Condition> getConditions() {
		return conditions;
	}
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	public String getGiteaHostname() {
		return giteaHostname;
	}
	public void setGiteaHostname(String giteaHostname) {
		this.giteaHostname = giteaHostname;
	}
	public String getGiteaRoute() {
		return giteaRoute;
	}
	public void setGiteaRoute(String giteaRoute) {
		this.giteaRoute = giteaRoute;
	}
	public String getUserPassword() {
		return userPassword;
	}
	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}
	public boolean isUserSetupComplete() {
		return userSetupComplete;
	}
	public void setUserSetupComplete(boolean userSetupComplete) {
		this.userSetupComplete = userSetupComplete;
	}
}
