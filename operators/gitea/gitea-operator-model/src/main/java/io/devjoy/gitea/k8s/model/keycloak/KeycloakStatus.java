package io.devjoy.gitea.k8s.model.keycloak;

import java.util.ArrayList;
import java.util.List;

public class KeycloakStatus {
    private List<String> availableApis = new ArrayList<>();
    private String expectedApi;
    
    public List<String> getAvailableApis() {
        return availableApis;
    }
    public void setAvailableApis(List<String> availableApis) {
        this.availableApis = availableApis;
    }
    public String getExpectedApi() {
        return expectedApi;
    }
    public void setExpectedApi(String expectedApi) {
        this.expectedApi = expectedApi;
    }

    

}
