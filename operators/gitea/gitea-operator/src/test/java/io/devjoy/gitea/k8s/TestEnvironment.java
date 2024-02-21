package io.devjoy.gitea.k8s;

import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;

import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class TestEnvironment {
    
	Optional<String> devServiceFlavor;

	private OpenShiftClient client;

	public TestEnvironment(OpenShiftClient client) {
		this.client = client;
		devServiceFlavor = ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class);
	}

	public TestEnvironment(OpenShiftClient client, Optional<String> devServiceFlavor) {
		this.devServiceFlavor = devServiceFlavor;
	}
    
    public void createStaticPVsIfRequired() {
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
