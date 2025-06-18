package io.devjoy.gitea.config;

import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

@Dependent
public class OpenShiftConfig {
    
    @Produces
    public OpenShiftClient openshiftClient() {
        return new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
    }
}
