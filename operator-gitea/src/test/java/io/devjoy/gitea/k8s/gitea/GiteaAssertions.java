package io.devjoy.gitea.k8s.gitea;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Comparator;
import java.util.Optional;

import org.hamcrest.core.IsNull;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresSecretDependentResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GiteaAssertions {
    @Inject
    OpenShiftClient client;

    public void assertPostgresPvc(Gitea desired) {
        final var postgresPvc = client.persistentVolumeClaims()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresPvcDependentResource.getName(desired)).get();
        assertThat(postgresPvc, is(IsNull.notNullValue()));
        if (!StringUtil.isNullOrEmpty(desired.getSpec().getPostgres().getStorageClass())) {
            assertThat(postgresPvc.getSpec().getStorageClassName(), is(desired.getSpec().getPostgres().getStorageClass()));
        }
        assertThat(postgresPvc.getSpec().getResources().getRequests().get("storage").toString(), is(desired.getSpec().getPostgres().getVolumeSize()));
        assertThat(postgresPvc.getStatus().getPhase(), is("Bound"));
    }

    public void assertGiteaPvc(Gitea desired) {
        final var giteaPvc = client.persistentVolumeClaims()
                    .inNamespace(desired.getMetadata().getNamespace())
                    .withName(GiteaPvcDependentResource.getName(desired)).get();
        assertThat(giteaPvc, is(IsNull.notNullValue()));
        if (!StringUtil.isNullOrEmpty(desired.getSpec().getStorageClass())) {
            assertThat(giteaPvc.getSpec().getStorageClassName(), is(desired.getSpec().getStorageClass()));
        }
        assertThat(giteaPvc.getSpec().getResources().getRequests().get("storage").toString(), is(desired.getSpec().getVolumeSize()));
        assertThat(giteaPvc.getStatus().getPhase(), is("Bound"));
    }

    public void assertAdminSecret(Gitea desired) {
        final var adminSecret = GiteaAdminSecretDependentResource.getResource(desired, client);
        assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("user"))), is(desired.getSpec().getAdminUser()));
        assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(IsNull.notNullValue()));
    }

    public void assertGitea(Gitea desired) {
        final var gitea = client.resources(Gitea.class)
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(desired.getMetadata().getName())
                .get();
        assertThat(gitea.getSpec().getAdminPassword(), is(IsNull.notNullValue()));
    }

    public void assertGiteaDeployment(Gitea desired) {
        final var postgresDeployment = client.apps().deployments()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresDeploymentDependentResource.getName(desired)).get();
        final var postgresPvc = client.persistentVolumeClaims()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresPvcDependentResource.getName(desired)).get();
        var postgresSecret = client.secrets()
                    .inNamespace(desired.getMetadata().getNamespace())
                    .withName(PostgresSecretDependentResource.getName(desired)).get();

        assertThat(postgresDeployment, is(IsNull.notNullValue()));
        assertThat(postgresSecret, is(IsNull.notNullValue()));
        assertThat(postgresPvc, is(IsNull.notNullValue()));
        assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
        assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
        assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getVolumes().stream().filter(v -> "postgresql-data".equals(v.getName())).map(v -> v.getPersistentVolumeClaim().getClaimName()).findFirst().get(), is(postgresPvc.getMetadata().getName()));
        
        assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
            .filter(e -> "POSTGRESQL_PASSWORD".equals(e.getName())).findAny().get().getValueFrom().getSecretKeyRef().getName(), is(postgresSecret.getMetadata().getName()));
        assertThat(postgresDeployment.getStatus().getReadyReplicas(), is(1));
        // RS
        Optional<ReplicaSet> postgresReplicaSet = client.apps().replicaSets()
            .inNamespace(desired.getMetadata().getNamespace())
            .list()
            .getItems()
            .stream()
            .filter(r -> r.getOwnerReferenceFor(postgresDeployment.getMetadata().getUid()).isPresent())
            .max(Comparator.comparingInt(r -> Integer.valueOf(r.getMetadata().getAnnotations().get("deployment.kubernetes.io/revision"))));
        assertThat(postgresReplicaSet.isPresent(), is(true));
        assertThat(postgresReplicaSet.get().getStatus().getReadyReplicas(), is(1));
        //Pod
        Optional<Pod> pod = client.pods()
            .inNamespace(desired.getMetadata().getNamespace())
            .list()
            .getItems()
            .stream()
            .filter(p -> p.getOwnerReferenceFor(postgresReplicaSet.get().getMetadata().getUid()).isPresent())
            .findAny();
        assertThat(pod.isPresent(), is(true));
        final var giteaDeployment = client.apps().deployments()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(desired.getMetadata().getName()).get();
        assertThat(giteaDeployment, is(IsNull.notNullValue()));
        assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
        assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
        assertThat(giteaDeployment.getStatus().getReadyReplicas(), is(1));
    }
}
