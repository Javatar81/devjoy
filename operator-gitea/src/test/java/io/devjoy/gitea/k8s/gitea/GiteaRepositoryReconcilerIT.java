package io.devjoy.gitea.k8s.gitea;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaSpec;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.gitea.repository.k8s.GiteaRepositorySpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class GiteaRepositoryReconcilerIT {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));

    @BeforeAll
	static void beforeAllTests() {
		Gitea gitea = createDefault("mygiteait-" + System.currentTimeMillis());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
	}

    @AfterAll
	static void tearDown() {
        client.resources(Gitea.class).delete();
		client.resources(GiteaRepository.class).delete();
	}

    @Test
    public void createPublicRepo() {
        GiteaRepository repo = createDefaultRepo("public");
        client.resource(repo).create();
        await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
			GiteaRepository giteaRepository = client.resources(GiteaRepository.class).inNamespace(client.getNamespace()).withName(repo.getMetadata().getName()).get();
            assertThat(giteaRepository, is(IsNull.notNullValue()));
            assertThat(giteaRepository.getStatus().getRepositoryCreated(), is(IsNull.notNullValue()));
        });
    }

    @Test
    public void createPrivateRepo() {
        GiteaRepository repo = createDefaultRepo("private");
        repo.getSpec().setVisibility(Visibility.PRIVATE);
        client.resource(repo).create();
        await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
			GiteaRepository giteaRepository = client.resources(GiteaRepository.class).inNamespace(client.getNamespace()).withName(repo.getMetadata().getName()).get();
            assertThat(giteaRepository, is(IsNull.notNullValue()));
            assertThat(giteaRepository.getStatus().getRepositoryCreated(), is(IsNull.notNullValue()));
        });
    }

    GiteaRepository createDefaultRepo(String name) {
        GiteaRepository repo = new GiteaRepository();
        repo.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(client.getNamespace())
                .build()); 
        GiteaRepositorySpec spec = new GiteaRepositorySpec();
        spec.setDeleteOnFinalize(false);
        spec.setUser("testuser");
        spec.setVisibility(Visibility.PUBLIC);
        repo.setSpec(spec);
        return repo;
    }

    static Gitea createDefault(String name) {
		Gitea gitea = new Gitea();
        gitea.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(client.getNamespace())
                .build()); 
		GiteaSpec spec = new GiteaSpec();
		spec.setAdminUser("devjoyITAdmin");
		spec.setAdminEmail("devjoyITAdmin@example.com");
		spec.setResourceRequirementsEnabled(false);
		spec.setIngressEnabled(client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE));
		spec.setSso(false);
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		spec.setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		gitea.setSpec(spec);
		return gitea;
	}
}
