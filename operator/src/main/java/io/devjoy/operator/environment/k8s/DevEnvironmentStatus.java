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
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((giteaCatalogSource == null) ? 0 : giteaCatalogSource.hashCode());
		result = prime * result + ((giteaSubscription == null) ? 0 : giteaSubscription.hashCode());
		result = prime * result + ((giteaResource == null) ? 0 : giteaResource.hashCode());
		result = prime * result + ((giteaAdminSecret == null) ? 0 : giteaAdminSecret.hashCode());
		result = prime * result + ((devSpaces == null) ? 0 : devSpaces.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DevEnvironmentStatus other = (DevEnvironmentStatus) obj;
		if (giteaCatalogSource == null) {
			if (other.giteaCatalogSource != null)
				return false;
		} else if (!giteaCatalogSource.equals(other.giteaCatalogSource))
			return false;
		if (giteaSubscription == null) {
			if (other.giteaSubscription != null)
				return false;
		} else if (!giteaSubscription.equals(other.giteaSubscription))
			return false;
		if (giteaResource == null) {
			if (other.giteaResource != null)
				return false;
		} else if (!giteaResource.equals(other.giteaResource))
			return false;
		if (giteaAdminSecret == null) {
			if (other.giteaAdminSecret != null)
				return false;
		} else if (!giteaAdminSecret.equals(other.giteaAdminSecret))
			return false;
		if (devSpaces == null) {
			if (other.devSpaces != null)
				return false;
		} else if (!devSpaces.equals(other.devSpaces))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DevEnvironmentStatus [giteaCatalogSource=" + giteaCatalogSource + ", giteaSubscription="
				+ giteaSubscription + ", giteaResource=" + giteaResource + ", giteaAdminSecret=" + giteaAdminSecret
				+ ", devSpaces=" + devSpaces + "]";
	}

	
	
}
