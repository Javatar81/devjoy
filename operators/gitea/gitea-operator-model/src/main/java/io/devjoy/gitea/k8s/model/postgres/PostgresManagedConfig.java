package io.devjoy.gitea.k8s.model.postgres;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class PostgresManagedConfig {
    @JsonPropertyDescription("The size of the volume to store Postgres data")
	@JsonProperty(defaultValue = "4Gi")
	private String volumeSize = "4Gi";
	@JsonPropertyDescription("The cpu resource limits for the Postgres deployment")
	private String cpuLimit;
	@JsonPropertyDescription("The cpu resource requests for the Postgres deployment")
	private String cpuRequest;
	@JsonPropertyDescription("The memory resource limits for the Postgres deployment")
	private String memoryLimit;
	@JsonPropertyDescription("The memory resource requests for the Postgres deployment")
	private String memoryRequest;
	@JsonPropertyDescription("The storage class used to store the Postgres data")
	private String storageClass;
	@JsonPropertyDescription("The image to be used for the Postgres pod")
	@JsonProperty(defaultValue = "registry.redhat.io/rhel9/postgresql-16")
	private String image = "registry.redhat.io/rhel9/postgresql-16";
	@JsonPropertyDescription("The image tag to be used for the Postgres pod")
	@JsonProperty(defaultValue = "latest")
	private String imageTag = "latest";
	@JsonPropertyDescription("Enables SSL for database connections.")
	private boolean ssl = false;

    public String getVolumeSize() {
		return volumeSize;
	}
	public void setVolumeSize(String volumeSize) {
		this.volumeSize = volumeSize;
	}
	public String getMemoryRequest() {
		return memoryRequest;
	}
	public void setMemoryRequest(String memoryRequest) {
		this.memoryRequest = memoryRequest;
	}
	public String getMemoryLimit() {
		return memoryLimit;
	}
	public void setMemoryLimit(String memoryLimit) {
		this.memoryLimit = memoryLimit;
	}
	public String getCpuRequest() {
		return cpuRequest;
	}
	public void setCpuRequest(String cpuRequest) {
		this.cpuRequest = cpuRequest;
	}
	public String getCpuLimit() {
		return cpuLimit;
	}
	public void setCpuLimit(String cpuLimit) {
		this.cpuLimit = cpuLimit;
	}
	public String getStorageClass() {
		return storageClass;
	}
	public void setStorageClass(String storageClass) {
		this.storageClass = storageClass;
	}
	public String getImage() {
		return image;
	}
	public void setImage(String image) {
		this.image = image;
	}
	public String getImageTag() {
		return imageTag;
	}
	public void setImageTag(String imageTag) {
		this.imageTag = imageTag;
	}
	public boolean isSsl() {
		return ssl;
	}
	public void setSsl(boolean giteaSsl) {
		this.ssl = giteaSsl;
	}
}
