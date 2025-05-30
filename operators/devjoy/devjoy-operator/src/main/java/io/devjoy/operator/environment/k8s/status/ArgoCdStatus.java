package io.devjoy.operator.environment.k8s.status;

import java.util.ArrayList;
import java.util.List;

public class ArgoCdStatus {
    
    private List<String> availableArgoCDApis = new ArrayList<>();
    private String expectedArgoCDApi;
    private String resourceName;
    private String host;
    private String phase;
    
    public List<String> getAvailableArgoCDApis() {
        return availableArgoCDApis;
    }
    public void setAvailableArgoCDApis(List<String> availableArgoCDApis) {
        this.availableArgoCDApis = availableArgoCDApis;
    }
    public String getExpectedArgoCDApi() {
        return expectedArgoCDApi;
    }
    public void setExpectedArgoCDApi(String expectedArgoCDApi) {
        this.expectedArgoCDApi = expectedArgoCDApi;
    }
    public String getResourceName() {
        return resourceName;
    }
    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }
    public String getHost() {
        return host;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public String getPhase() {
        return phase;
    }
    public void setPhase(String phase) {
        this.phase = phase;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((availableArgoCDApis == null) ? 0 : availableArgoCDApis.hashCode());
        result = prime * result + ((expectedArgoCDApi == null) ? 0 : expectedArgoCDApi.hashCode());
        result = prime * result + ((resourceName == null) ? 0 : resourceName.hashCode());
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((phase == null) ? 0 : phase.hashCode());
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
        ArgoCdStatus other = (ArgoCdStatus) obj;
        if (availableArgoCDApis == null) {
            if (other.availableArgoCDApis != null)
                return false;
        } else if (!availableArgoCDApis.equals(other.availableArgoCDApis))
            return false;
        if (expectedArgoCDApi == null) {
            if (other.expectedArgoCDApi != null)
                return false;
        } else if (!expectedArgoCDApi.equals(other.expectedArgoCDApi))
            return false;
        if (resourceName == null) {
            if (other.resourceName != null)
                return false;
        } else if (!resourceName.equals(other.resourceName))
            return false;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (phase == null) {
            if (other.phase != null)
                return false;
        } else if (!phase.equals(other.phase))
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ArgoCdStatus [availableArgoCDApis=" + availableArgoCDApis + ", expectedArgoCDApi=" + expectedArgoCDApi
                + ", resourceName=" + resourceName + ", host=" + host + ", phase=" + phase + "]";
    }


}
