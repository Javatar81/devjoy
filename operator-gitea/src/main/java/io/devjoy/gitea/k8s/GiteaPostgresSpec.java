package io.devjoy.gitea.k8s;

public class GiteaPostgresSpec {
	private String volumeSize;
	private String memoryRequest;
	private String memoryLimit;
	private String cpuRequest;
	private String cpuLimit;
	private String storageClass;
	private String image;
	private String imageTag;
	
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
}
