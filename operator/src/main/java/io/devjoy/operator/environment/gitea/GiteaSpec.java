package io.devjoy.operator.environment.gitea;

import java.util.List;

public class GiteaSpec {
	private int giteaUserNumber;
	private String giteaAdminEmail;
	private int giteaAdminPasswordLength;
	private boolean giteaMigrateRepositories;
	private String postgresqlCpuLimit;
	private String giteaVolumeSize;
	private String giteaAdminUser;
	private String giteaImageTag;
	private String postgresqlVolumeSize;
	private String giteaCpuRequest;
	private String postgresqlCpuRequest;
	private String giteaAdminPassword;
	private boolean giteaCreateUsers;
	private boolean giteaSsl;
	private String giteaGenerateUserFormat;
	private String giteaUserPassword;
	private List<GiteaRepositoryItem> giteaRepositoriesList;
	
	public int getGiteaUserNumber() {
		return giteaUserNumber;
	}
	public void setGiteaUserNumber(int giteaUserNumber) {
		this.giteaUserNumber = giteaUserNumber;
	}
	public String getGiteaAdminEmail() {
		return giteaAdminEmail;
	}
	public void setGiteaAdminEmail(String giteaAdminEmail) {
		this.giteaAdminEmail = giteaAdminEmail;
	}
	public int getGiteaAdminPasswordLength() {
		return giteaAdminPasswordLength;
	}
	public void setGiteaAdminPasswordLength(int giteaAdminPasswordLength) {
		this.giteaAdminPasswordLength = giteaAdminPasswordLength;
	}
	public boolean isGiteaMigrateRepositories() {
		return giteaMigrateRepositories;
	}
	public void setGiteaMigrateRepositories(boolean giteaMigrateRepositories) {
		this.giteaMigrateRepositories = giteaMigrateRepositories;
	}
	public String getPostgresqlCpuLimit() {
		return postgresqlCpuLimit;
	}
	public void setPostgresqlCpuLimit(String postgresqlCpuLimit) {
		this.postgresqlCpuLimit = postgresqlCpuLimit;
	}
	public String getGiteaVolumeSize() {
		return giteaVolumeSize;
	}
	public void setGiteaVolumeSize(String giteaVolumeSize) {
		this.giteaVolumeSize = giteaVolumeSize;
	}
	public String getGiteaAdminUser() {
		return giteaAdminUser;
	}
	public void setGiteaAdminUser(String giteaAdminUser) {
		this.giteaAdminUser = giteaAdminUser;
	}
	public String getGiteaImageTag() {
		return giteaImageTag;
	}
	public void setGiteaImageTag(String giteaImageTag) {
		this.giteaImageTag = giteaImageTag;
	}
	public String getPostgresqlVolumeSize() {
		return postgresqlVolumeSize;
	}
	public void setPostgresqlVolumeSize(String postgresqlVolumeSize) {
		this.postgresqlVolumeSize = postgresqlVolumeSize;
	}
	public String getGiteaCpuRequest() {
		return giteaCpuRequest;
	}
	public void setGiteaCpuRequest(String giteaCpuRequest) {
		this.giteaCpuRequest = giteaCpuRequest;
	}
	public String getPostgresqlCpuRequest() {
		return postgresqlCpuRequest;
	}
	public void setPostgresqlCpuRequest(String postgresqlCpuRequest) {
		this.postgresqlCpuRequest = postgresqlCpuRequest;
	}
	public String getGiteaAdminPassword() {
		return giteaAdminPassword;
	}
	public void setGiteaAdminPassword(String giteaAdminPassword) {
		this.giteaAdminPassword = giteaAdminPassword;
	}
	public boolean isGiteaCreateUsers() {
		return giteaCreateUsers;
	}
	public void setGiteaCreateUsers(boolean giteaCreateUsers) {
		this.giteaCreateUsers = giteaCreateUsers;
	}
	public boolean isGiteaSsl() {
		return giteaSsl;
	}
	public void setGiteaSsl(boolean giteaSsl) {
		this.giteaSsl = giteaSsl;
	}
	public String getGiteaGenerateUserFormat() {
		return giteaGenerateUserFormat;
	}
	public void setGiteaGenerateUserFormat(String giteaGenerateUserFormat) {
		this.giteaGenerateUserFormat = giteaGenerateUserFormat;
	}
	public String getGiteaUserPassword() {
		return giteaUserPassword;
	}
	public void setGiteaUserPassword(String giteaUserPassword) {
		this.giteaUserPassword = giteaUserPassword;
	}
	public List<GiteaRepositoryItem> getGiteaRepositoriesList() {
		return giteaRepositoriesList;
	}
	public void setGiteaRepositoriesList(List<GiteaRepositoryItem> giteaRepositoriesList) {
		this.giteaRepositoriesList = giteaRepositoriesList;
	}
}
