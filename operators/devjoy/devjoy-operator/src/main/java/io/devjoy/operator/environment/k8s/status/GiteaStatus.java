package io.devjoy.operator.environment.k8s.status;

import java.util.ArrayList;
import java.util.List;

public class GiteaStatus {

    private String resourceName;
    private String giteaAdminSecret;
    private List<String> availableGiteaApis = new ArrayList<>();
    private String expectedGiteaApi;
    
    public String getExpectedGiteaApi() {
        return expectedGiteaApi;
    }
    public void setExpectedGiteaApi(String expectedGiteaApi) {
        this.expectedGiteaApi = expectedGiteaApi;
    }
    public String getGiteaAdminSecret() {
        return giteaAdminSecret;
    }
    public void setGiteaAdminSecret(String giteaAdminSecret) {
        this.giteaAdminSecret = giteaAdminSecret;
    }
    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public List<String> getAvailableGiteaApis() {
        return availableGiteaApis;
    }
    public void setAvailableGiteaApis(List<String> availableGiteaApis) {
        this.availableGiteaApis = availableGiteaApis;
    }
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
        result = prime * result + ((giteaAdminSecret == null) ? 0 : giteaAdminSecret.hashCode());
        result = prime * result + ((availableGiteaApis == null) ? 0 : availableGiteaApis.hashCode());
        result = prime * result + ((expectedGiteaApi == null) ? 0 : expectedGiteaApi.hashCode());
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
        GiteaStatus other = (GiteaStatus) obj;
        if (resourceName == null) {
            if (other.resourceName != null)
                return false;
        } else if (!resourceName.equals(other.resourceName))
            return false;
        if (giteaAdminSecret == null) {
            if (other.giteaAdminSecret != null)
                return false;
        } else if (!giteaAdminSecret.equals(other.giteaAdminSecret))
            return false;
        if (availableGiteaApis == null) {
            if (other.availableGiteaApis != null)
                return false;
        } else if (!availableGiteaApis.equals(other.availableGiteaApis))
            return false;
        if (expectedGiteaApi == null) {
            if (other.expectedGiteaApi != null)
                return false;
        } else if (!expectedGiteaApi.equals(other.expectedGiteaApi))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "GiteaStatus [resourceName=" + resourceName + ", giteaAdminSecret=" + giteaAdminSecret
                + ", availableGiteaApis=" + availableGiteaApis + ", expectedGiteaApi=" + expectedGiteaApi + "]";
    }

	
}
