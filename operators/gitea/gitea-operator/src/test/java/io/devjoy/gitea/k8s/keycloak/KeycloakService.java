package io.devjoy.gitea.k8s.keycloak;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Map;

import org.keycloak.k8s.v2alpha1.Keycloak;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImport;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.Placeholders;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.Realm;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class KeycloakService {

    private final OpenShiftClient client;
    private final String targetNamespace;
    private final ObjectMapper mapper = new ObjectMapper();
    
    public KeycloakService(OpenShiftClient client, String targetNamespace) {
        this.client = client;
        this.targetNamespace = targetNamespace;
    }

    public Keycloak newKeycloak() {
        var keycloak = client.resources(Keycloak.class)
                .load(getClass().getClassLoader().getResourceAsStream("keycloak-k8s/keycloak.yaml"))
                .item();
        keycloak.getMetadata().setNamespace(targetNamespace);
        keycloak.getSpec().getHostname().setHostname(getHostname());
        return keycloak;
    }

    public Secret newTlsSecret() throws IOException {
        try (BufferedInputStream certStream = new BufferedInputStream(
                getClass().getClassLoader().getResourceAsStream("keycloak-k8s/certificate.pem"));
                BufferedInputStream keyStream = new BufferedInputStream(
                        getClass().getClassLoader().getResourceAsStream("keycloak-k8s/key.pem"))) {
            return new SecretBuilder()
                    .withType("kubernetes.io/tls")
                    .withNewMetadata().withNamespace(targetNamespace).withName("my-tls-secret").endMetadata()
                    .addToStringData("tls.crt", new String(certStream.readAllBytes()))
                    .addToStringData("tls.key", new String(keyStream.readAllBytes()))
                    .build();
        }
    }

    public KeycloakRealmImport newRealmImport(String oidcSecretName, String oidcSecretKey) throws IOException {
        KeycloakRealmImport keycloakRealmImport = client.resources(KeycloakRealmImport.class)
					.load(getClass().getClassLoader().getResourceAsStream("keycloak-k8s/keycloak-realm.yaml"))
					.item();
		keycloakRealmImport.getMetadata().setNamespace(targetNamespace);
		keycloakRealmImport.getSpec().setRealm(mapper.readValue(getClass().getClassLoader().getResourceAsStream("keycloak-k8s/realm-export.json"), Realm.class));
        var placeholderSecret = new org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.placeholders.Secret(); 
			placeholderSecret.setName(oidcSecretName);
			placeholderSecret.setKey(oidcSecretKey);
			Placeholders secretPlaceholder = new Placeholders();
			secretPlaceholder.setSecret(placeholderSecret);
			keycloakRealmImport.getSpec().setPlaceholders(Map.of("DEVJOY_GITEA_CLIENT_SECRET", secretPlaceholder));
        return keycloakRealmImport;
    }

    public String getHostname() {
        String baseDomain = client.config().ingresses().withName("cluster").get().getSpec().getDomain();
        return String.format("https://%s-%s.%s", "mygiteait-devjoy", targetNamespace, baseDomain);
    }
}
