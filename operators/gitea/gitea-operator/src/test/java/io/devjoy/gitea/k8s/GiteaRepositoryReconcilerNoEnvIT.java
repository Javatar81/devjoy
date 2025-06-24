package io.devjoy.gitea.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositorySpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;


@QuarkusTest
class GiteaRepositoryReconcilerNoEnvIT {

    static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
    static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));
    
    @AfterEach
	void tearDown() {
		client.resources(GiteaRepository.class).inNamespace(getTargetNamespace()).delete();
        client.resources(Gitea.class).inNamespace(getTargetNamespace()).delete();
	}

    @Test
    void createRepoWithoutEnvInNamespace() {
        GiteaRepository repo = createDefaultRepo("nogitea");
        client.resource(repo).create();
        await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
			GiteaRepository giteaRepository = client.resources(GiteaRepository.class).inNamespace(getTargetNamespace()).withName(repo.getMetadata().getName()).get();
            assertThat(giteaRepository, is(IsNull.notNullValue()));
            assertThat(giteaRepository.getStatus().getRepositoryCreated(), is(IsNull.nullValue()));
        });
    }

    @Test
    void associateRepoWithEnvInNamespace() {
        Gitea gitea = createDefaultEnv("associatenamespace");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
        
        GiteaRepository repo = createDefaultRepo("giteainnamespace");
        client.resource(repo).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
			GiteaRepository giteaRepository = client.resources(GiteaRepository.class).inNamespace(getTargetNamespace()).withName(repo.getMetadata().getName()).get();
            assertThat(giteaRepository, is(IsNull.notNullValue()));
            assertThat(giteaRepository.getStatus().getRepositoryCreated(), is(IsNull.notNullValue()));
        });
    }

    @Test
    void createRepositoryBeforeGitea() {
        GiteaRepository repo = createDefaultRepo("repofirst");
        client.resource(repo).create();
        Gitea gitea = createDefaultEnv("laterenv");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
			GiteaRepository giteaRepository = client.resources(GiteaRepository.class).inNamespace(getTargetNamespace()).withName(repo.getMetadata().getName()).get();
            assertThat(giteaRepository, is(IsNull.notNullValue()));
            assertThat(giteaRepository.getStatus().getRepositoryCreated(), is(IsNull.notNullValue()));
        });
    }

    GiteaRepository createDefaultRepo(String name) {
        GiteaRepository repo = new GiteaRepository();
        repo.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(getTargetNamespace())
                .build()); 
        GiteaRepositorySpec spec = new GiteaRepositorySpec();
        spec.setDeleteOnFinalize(true);
        spec.setUser("testuser");
        spec.setVisibility(Visibility.PUBLIC);
        repo.setSpec(spec);
        return repo;
    }

    static Gitea createDefaultEnv(String name) {
		Gitea gitea = new Gitea();
        gitea.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(getTargetNamespace())
                .build()); 
		GiteaSpec spec = new GiteaSpec();
		spec.setAdminUser("devjoyITAdmin");
		spec.setAdminEmail("devjoyITAdmin@example.com");
		spec.setResourceRequirementsEnabled(false);
		spec.setIngressEnabled(client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE));
		spec.setSso(false);
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().getManagedConfig().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		spec.setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		gitea.setSpec(spec);
		return gitea;
	}

    private static String getTargetNamespace() {
		return client.getNamespace() + "2";
	}
}