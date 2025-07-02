package io.devjoy.gitea.k8s.model.keycloak;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class KeycloakUnmanagedConfig {
    @JsonPropertyDescription("The OpenId Connect client name.")
    private String oidcClient;
    @JsonPropertyDescription("The secret containing the password. Key must be 'secret'.")
	private String oidcExtraSecretName;
    @JsonPropertyDescription("The discover url for OpenId Connect.")
    private String oidcAutoDiscoverUrl;

    public String getOidcClient() {
        return oidcClient;
    }
    public void setOidcClient(String oidcClient) {
        this.oidcClient = oidcClient;
    }
    public String getOidcExtraSecretName() {
        return oidcExtraSecretName;
    }
    public void setOidcExtraSecretName(String oidcExtraSecretName) {
        this.oidcExtraSecretName = oidcExtraSecretName;
    }
    public String getOidcAutoDiscoverUrl() {
        return oidcAutoDiscoverUrl;
    }
    public void setOidcAutoDiscoverUrl(String oidcAutoDiscoverUrl) {
        this.oidcAutoDiscoverUrl = oidcAutoDiscoverUrl;
    }
   
    

}
