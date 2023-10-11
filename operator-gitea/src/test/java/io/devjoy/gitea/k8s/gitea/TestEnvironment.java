package io.devjoy.gitea.k8s.gitea;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class TestEnvironment {
    
    @ConfigProperty(name = "test.quarkus.kubernetes-client.devservices.flavor")
	Optional<String> devServiceFlavor;

    @Inject
	OpenShiftClient client;
    
    void createStaticPVsIfRequired() {
		devServiceFlavor.filter(f -> "k3s".equalsIgnoreCase(f)).ifPresent(f -> {
			PersistentVolume pv1 = client.persistentVolumes()
				.load(getClass().getClassLoader().getResourceAsStream("k3s/pv1.yaml"))
				.item();
		client.resource(pv1).create();
			PersistentVolume pv2 = client.persistentVolumes()
					.load(getClass().getClassLoader().getResourceAsStream("k3s/pv2.yaml"))
					.item();
			client.resource(pv2).create();
		});
	}
}
