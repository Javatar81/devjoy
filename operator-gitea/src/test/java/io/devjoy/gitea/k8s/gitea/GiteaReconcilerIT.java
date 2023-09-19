package io.devjoy.gitea.k8s.gitea;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hamcrest.core.IsNull;
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
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
class GiteaReconcilerIT {
	
	@Inject
	OpenShiftClient client;

	@ConfigProperty(name = "test.quarkus.kubernetes-client.devservices.flavor")
	Optional<String> devServiceFlavor;

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
		await().ignoreException(NullPointerException.class).atMost(60, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			final var postgresPvc = client.persistentVolumeClaims()
                    .inNamespace(gitea.getMetadata().getNamespace())
                    .withName(PostgresPvcDependentResource.getName(gitea)).get();
			assertThat(postgresPvc, is(IsNull.notNullValue()));
			assertThat(postgresPvc.getSpec().getStorageClassName(), is(gitea.getSpec().getPostgres().getStorageClass()));
			assertThat(postgresPvc.getSpec().getResources().getRequests().get("storage"), is(volumeSize));
			assertThat(postgresPvc.getStatus().getPhase(), is("Bound"));
			final var giteaPvc = client.persistentVolumeClaims()
                    .inNamespace(gitea.getMetadata().getNamespace())
                    .withName(GiteaPvcDependentResource.getName(gitea)).get();
			assertThat(giteaPvc, is(IsNull.notNullValue()));
			assertThat(giteaPvc.getSpec().getStorageClassName(), is(gitea.getSpec().getStorageClass()));
			assertThat(giteaPvc.getSpec().getResources().getRequests().get("storage"), is(volumeSize));
			assertThat(giteaPvc.getStatus().getPhase(), is("Bound"));
			// Postgres Secret
			var postgresSecret = client.secrets()
                    .inNamespace(gitea.getMetadata().getNamespace())
                    .withName(PostgresSecretDependentResource.getName(gitea)).get();
			assertThat(postgresSecret, is(IsNull.notNullValue()));


			final var postgresDeployment = client.apps().deployments()
                    .inNamespace(gitea.getMetadata().getNamespace())
                    .withName(PostgresDeploymentDependentResource.getName(gitea)).get();
			assertThat(postgresDeployment, is(IsNull.notNullValue()));
			assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
			assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
			assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getVolumes().stream().filter(v -> "postgresql-data".equals(v.getName())).map(v -> v.getPersistentVolumeClaim().getClaimName()).findFirst().get(), is(postgresPvc.getMetadata().getName()));
			
			assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
				.filter(e -> "POSTGRESQL_PASSWORD".equals(e.getName())).findAny().get().getValueFrom().getSecretKeyRef().getName(), is(postgresSecret.getMetadata().getName()));
			assertThat(postgresDeployment.getStatus().getReadyReplicas(), is(1));
			// RS
			Optional<ReplicaSet> postgresReplicaSet = client.apps().replicaSets()
				.inNamespace(gitea.getMetadata().getNamespace())
				.list()
				.getItems()
				.stream()
				.filter(r -> r.getOwnerReferenceFor(postgresDeployment.getMetadata().getUid()).isPresent())
				.max(Comparator.comparingInt(r -> Integer.valueOf(r.getMetadata().getAnnotations().get("deployment.kubernetes.io/revision"))));
			assertThat(postgresReplicaSet.isPresent(), is(true));
			assertThat(postgresReplicaSet.get().getStatus().getReadyReplicas(), is(1));
			//Pod
			Optional<Pod> pod = client.pods()
				.inNamespace(gitea.getMetadata().getNamespace())
				.list()
				.getItems()
				.stream()
				.filter(p -> p.getOwnerReferenceFor(postgresReplicaSet.get().getMetadata().getUid()).isPresent())
				.findAny();
			assertThat(pod.isPresent(), is(true));
			final var giteaDeployment = client.apps().deployments()
                    .inNamespace(gitea.getMetadata().getNamespace())
                    .withName(gitea.getMetadata().getName()).get();
			assertThat(giteaDeployment, is(IsNull.notNullValue()));
			assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
			assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
			assertThat(giteaDeployment.getStatus().getReadyReplicas(), is(1));
        });
	}

	
}
