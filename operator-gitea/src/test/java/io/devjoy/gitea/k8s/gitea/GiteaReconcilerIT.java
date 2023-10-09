package io.devjoy.gitea.k8s.gitea;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaSpec;
import io.devjoy.gitea.k8s.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresSecretDependentResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.PersistentVolume;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;

import static org.hamcrest.MatcherAssert.assertThat;

import io.quarkus.runtime.util.StringUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

@QuarkusTest
class GiteaReconcilerIT {
	
	@Inject
	OpenShiftClient client;

	private GiteaAssertions assertions;

	@ConfigProperty(name = "test.quarkus.kubernetes-client.devservices.flavor")
	Optional<String> devServiceFlavor;

	@PostConstruct
	public void setUp() {
		assertions = new GiteaAssertions(client);
	}

	@AfterEach
	void tearDown() {
		client.resources(Gitea.class).withName("mygiteait").delete();
	}

	@Test
	void createGitea() {
		
		Gitea gitea = new Gitea();
		gitea.setMetadata(new ObjectMetaBuilder()
		        .withName("mygiteait")
		        .withNamespace(client.getNamespace())
		        .build()); 
		GiteaSpec spec = new GiteaSpec();
		spec.setAdminUser("devjoyITAdmin");
		spec.setAdminEmail("devjoyITAdmin@example.com");
		spec.setResourceRequirementsEnabled(false);
		spec.setIngressEnabled(client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE));
		spec.setSso(false);
		// Required to run on k3s
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		spec.setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		gitea.setSpec(spec);
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
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
        });
	}

	
}
