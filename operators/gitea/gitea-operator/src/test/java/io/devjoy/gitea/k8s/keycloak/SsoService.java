package io.devjoy.gitea.k8s.keycloak;

import java.util.List;

import org.keycloak.v1alpha1.Keycloak;
import org.keycloak.v1alpha1.KeycloakClient;
import org.keycloak.v1alpha1.KeycloakRealm;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.openshift.client.OpenShiftClient;

public class SsoService {

    private final OpenShiftClient client;
    private final String targetNamespace;
    private final ObjectMapper mapper = new ObjectMapper();
    
    public SsoService(OpenShiftClient client, String targetNamespace) {
        this.client = client;
        this.targetNamespace = targetNamespace;
    }

    public Keycloak newKeycloak() {
        var keycloak = client.resources(Keycloak.class)
                .load(getClass().getClassLoader().getResourceAsStream("sso/keycloak.yaml"))
                .item();
        keycloak.getMetadata().setNamespace(targetNamespace);
        return keycloak;
    }

    public KeycloakRealm newKeycloakRealm() {
        var keycloakRealm = client.resources(KeycloakRealm.class)
                .load(getClass().getClassLoader().getResourceAsStream("sso/keycloak-realm.yaml"))
                .item();
                keycloakRealm.getMetadata().setNamespace(targetNamespace);
        return keycloakRealm;
    }
//mygiteait
    public KeycloakClient newKeycloakClient(String giteaHost, String clientId, String secret) {
        var keycloakClient = client.resources(KeycloakClient.class)
                .load(getClass().getClassLoader().getResourceAsStream("sso/keycloak-client.yaml"))
                .item();
        keycloakClient.getMetadata().setNamespace(targetNamespace);
        String internalRedirectUri = String.format("http://%s.%s.svc.cluster.local:3000", giteaHost, targetNamespace);
        String baseDomain = client.config().ingresses().withName("cluster").get().getSpec().getDomain();
        String externalRedirectUri = String.format("http://%s-%s.%s/user/oauth2/devjoy-oidc/callback", giteaHost, targetNamespace, baseDomain);
        var clnt = keycloakClient.getSpec().getClient();
        clnt.setRedirectUris(List.of(internalRedirectUri, externalRedirectUri));
        clnt.setSecret(secret);
        clnt.setClientId(clientId);        
        return keycloakClient;
    }

    public String getHostname() {
        String baseDomain = client.config().ingresses().withName("cluster").get().getSpec().getDomain();
        return String.format("https://%s-%s.%s", "keycloak", targetNamespace, baseDomain);
    }
}
