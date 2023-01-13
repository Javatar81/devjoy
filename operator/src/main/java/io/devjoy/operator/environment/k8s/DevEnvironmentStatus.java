package io.devjoy.operator.environment.k8s;

public class DevEnvironmentStatus {

    private String giteaCatalogSource;
    private String giteaSubscription;
    private String giteaResource;
    private String giteaAdminSecret;
    private DevSpacesStatus devSpaces;
    
	public String getGiteaCatalogSource() {
		return giteaCatalogSource;
	}
	public void setGiteaCatalogSource(String giteaCatalogSource) {
		this.giteaCatalogSource = giteaCatalogSource;
	}
	public String getGiteaSubscription() {
		return giteaSubscription;
	}
	public void setGiteaSubscription(String giteaSubscription) {
		this.giteaSubscription = giteaSubscription;
	}
	public String getGiteaResource() {
		return giteaResource;
	}
	public void setGiteaResource(String giteaResource) {
		this.giteaResource = giteaResource;
	}
	public String getGiteaAdminSecret() {
		return giteaAdminSecret;
	}
	public void setGiteaAdminSecret(String giteaAdminSecret) {
		this.giteaAdminSecret = giteaAdminSecret;
	}
	public DevSpacesStatus getDevSpaces() {
		return devSpaces;
	}
	public void setDevSpaces(DevSpacesStatus devSpaces) {
		this.devSpaces = devSpaces;
	}
	
}
