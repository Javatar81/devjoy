package io.devjoy.operator.project.k8s;

public class WorkspaceStatus {
	
	private String factoryUrl;
	private InitStatus initStatus;

	public String getFactoryUrl() {
		return factoryUrl;
	}
	public void setFactoryUrl(String factoryUrl) {
		this.factoryUrl = factoryUrl;
	}
	public InitStatus getInitStatus() {
		return initStatus;
	}
	public void setInitStatus(InitStatus initStatus) {
		this.initStatus = initStatus;
	}
	
}
