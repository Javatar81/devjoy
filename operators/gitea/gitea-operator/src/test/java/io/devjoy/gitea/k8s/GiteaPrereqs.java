package io.devjoy.gitea.k8s;

import org.keycloak.v1alpha1.Keycloak;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class GiteaPrereqs {
    private static final Logger LOG = LoggerFactory.getLogger(GiteaPrereqs.class);
    static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);

    public void assureKeycloakCrdsInstalled() {
        LOG.error("Assure Keycloak CRDs installed");
        if (client.apiextensions().getApiGroup("keycloak.org") == null) {
            LOG.error("Keycloak CRDs not available. Will be installed.");
            Keycloak keycloak = client.resources(Keycloak.class)
				.load(getClass().getClassLoader().getResourceAsStream("crds/keycloak/keycloak.yaml")).item();
            client.resource(keycloak).create();
        }
    }
}
