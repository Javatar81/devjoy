package io.devjoy.gitea.k8s.model.keycloak;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class KeycloakSpec {
   
   @JsonPropertyDescription("If sso configuration with Keycloak is enabled.")
   private boolean enabled = false;
   @JsonPropertyDescription("Defines if the Keycloak instance is managed by this operator. Only active if enabled == true.")
   private boolean managed = false;
   @JsonPropertyDescription("The config to integrate Gitea with an existing and unmanaged Keycloak.")
   private KeycloakUnmanaged unmanagedConfig;
   
   public boolean isEnabled() {
    return enabled;
   }
   public void setEnabled(boolean enabled) {
    this.enabled = enabled;
   }
   public boolean isManaged() {
    return managed;
   }
   public void setManaged(boolean managed) {
    this.managed = managed;
    if(managed) {
        this.unmanagedConfig = null;
    }
   }
   public KeycloakUnmanaged getUnmanagedConfig() {
    return unmanagedConfig;
   }
   public void setUnmanagedConfig(KeycloakUnmanaged unmanagedConfig) {
    this.unmanagedConfig = unmanagedConfig;
   } 

    

}
