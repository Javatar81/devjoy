package io.devjoy.gitea.k8s;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class GiteaSpec {
	@JsonPropertyDescription("The name of the admin user")
	private String adminUser;
	@JsonPropertyDescription("The email of the admin user")
	private String adminEmail;
	@JsonPropertyDescription("The optional admin password. If not set it will be generated. Once set the value will be moved to a secret.")
	private String adminPassword;
	@JsonPropertyDescription("The length of the generated admin password. Value is ignored if adminPassword is set. Min length is 10.")
	private int adminPasswordLength;
	@JsonPropertyDescription("Size of the storage request of the persistent volume claim. Default value is 4Gi.")
	private String volumeSize;
	@JsonPropertyDescription("Storage class of the persistent volume claim.")
	private String storageClass;
	@JsonPropertyDescription("The image url to use for Gitea. Default is quay.io/gpte-devops-automation/gitea")
	private String image;
	@JsonPropertyDescription("The image tag to use for Gitea. Default is latest")
	private String imageTag;
	private String cpuLimit;
	private String cpuRequest;
	private String memoryLimit;
	private String memoryRequest;
	@JsonPropertyDescription("Enables resource requirements such as cpuLimit, cpuRequest, memoryLimit, and memoryRequest. Default value is true.")
	private boolean resourceRequirementsEnabled;
	private boolean registerEmailConfirm;
	private boolean disableRegistration;
	private boolean enableCaptcha;
	private boolean allowCreateOrganization;
	@JsonPropertyDescription("Create a route / ingress to access for Gitea.")
	private boolean ingressEnabled = true;
	@JsonPropertyDescription("Enables SSL for Gitea.")
	private boolean ssl;
	@JsonPropertyDescription("Enables SSO using RHSSO and OpenShift.")
	private boolean sso;
	@JsonPropertyDescription("The hostname of the route. If not set it will be generated by OpenShift.")
	private String route;
	@JsonPropertyDescription("The log level for Gitea. Default is Warn.")
	private GiteaLogLevel logLevel;
	private boolean migrateRepositories;
	private List<GiteaRepositoryItem> repositoriesList;
	private GiteaPostgresSpec postgres = new GiteaPostgresSpec();
	private GiteaMailerSpec mailer = new GiteaMailerSpec();
	private GiteaConfigOverrides configOverrides = new GiteaConfigOverrides(); 
	
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
	public String getVolumeSize() {
		return volumeSize;
	}
	public void setVolumeSize(String giteaVolumeSize) {
		this.volumeSize = giteaVolumeSize;
	}
	public String getStorageClass() {
		return storageClass;
	}
	public void setStorageClass(String giteaStorageClass) {
		this.storageClass = giteaStorageClass;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String giteaImage) {
		this.image = giteaImage;
	}
	public String getImageTag() {
		return imageTag;
	}
	public void setImageTag(String giteaImageTag) {
		this.imageTag = giteaImageTag;
	}
	public String getCpuLimit() {
		return cpuLimit;
	}
	public void setCpuLimit(String giteaCpuLimit) {
		this.cpuLimit = giteaCpuLimit;
	}
	public String getCpuRequest() {
		return cpuRequest;
	}
	public void setCpuRequest(String giteaCpuRequest) {
		this.cpuRequest = giteaCpuRequest;
	}
	public String getMemoryLimit() {
		return memoryLimit;
	}
	public void setMemoryLimit(String giteaMemoryLimit) {
		this.memoryLimit = giteaMemoryLimit;
	}
	public String getMemoryRequest() {
		return memoryRequest;
	}
	public void setMemoryRequest(String giteaMemoryRequest) {
		this.memoryRequest = giteaMemoryRequest;
	}
	public boolean isResourceRequirementsEnabled() {
		return resourceRequirementsEnabled;
	}
	public void setResourceRequirementsEnabled(boolean resourceRequirementsEnabled) {
		this.resourceRequirementsEnabled = resourceRequirementsEnabled;
	}
	public boolean isRegisterEmailConfirm() {
		return registerEmailConfirm;
	}
	public void setRegisterEmailConfirm(boolean registerEmailConfirm) {
		this.registerEmailConfirm = registerEmailConfirm;
	}
	public boolean isDisableRegistration() {
		return disableRegistration;
	}
	public void setDisableRegistration(boolean disableRegistration) {
		this.disableRegistration = disableRegistration;
	}
	public boolean isEnableCaptcha() {
		return enableCaptcha;
	}
	public void setEnableCaptcha(boolean enableCaptcha) {
		this.enableCaptcha = enableCaptcha;
	}
	public boolean isAllowCreateOrganization() {
		return allowCreateOrganization;
	}
	public void setAllowCreateOrganization(boolean allowCreateOrganization) {
		this.allowCreateOrganization = allowCreateOrganization;
	}
	public boolean isSsl() {
		return ssl;
	}
	public void setSsl(boolean giteaSsl) {
		this.ssl = giteaSsl;
	}
	public boolean isSso() {
		return sso;
	}
	public void setSso(boolean sso) {
		this.sso = sso;
	}
	public String getRoute() {
		return route;
	}
	public void setRoute(String giteaRoute) {
		this.route = giteaRoute;
	}
	public GiteaLogLevel getLogLevel() {
		return logLevel;
	}
	public void setLogLevel(GiteaLogLevel logLevel) {
		this.logLevel = logLevel;
	}
	public boolean isMigrateRepositories() {
		return migrateRepositories;
	}
	public void setMigrateRepositories(boolean giteaMigrateRepositories) {
		this.migrateRepositories = giteaMigrateRepositories;
	}
	public List<GiteaRepositoryItem> getRepositoriesList() {
		return repositoriesList;
	}
	public void setRepositoriesList(List<GiteaRepositoryItem> giteaRepositoriesList) {
		this.repositoriesList = giteaRepositoriesList;
	}
	public GiteaPostgresSpec getPostgres() {
		return postgres;
	}
	public void setPostgres(GiteaPostgresSpec postgres) {
		this.postgres = postgres;
	}
	public GiteaMailerSpec getMailer() {
		return mailer;
	}
	public void setMailer(GiteaMailerSpec mailer) {
		this.mailer = mailer;
	}
	public GiteaConfigOverrides getConfigOverrides() {
		return configOverrides;
	}
	public void setConfigOverrides(GiteaConfigOverrides configOverrides) {
		this.configOverrides = configOverrides;
	}
	public boolean isIngressEnabled() {
		return ingressEnabled;
	}
	public void setIngressEnabled(boolean ingressEnabled) {
		this.ingressEnabled = ingressEnabled;
	}
	
}
