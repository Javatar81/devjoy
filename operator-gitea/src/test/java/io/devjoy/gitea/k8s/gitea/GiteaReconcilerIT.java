package io.devjoy.gitea.k8s.gitea;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.devjoy.gitea.domain.TokenService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaSpec;
import io.devjoy.gitea.k8s.rhsso.KeycloakDependentResource;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class GiteaReconcilerIT {
    @Inject
	OpenShiftClient client;
	@Inject
	TokenService tokenService;
	@Inject
	TestEnvironment env;
	@Inject
	GiteaAssertions assertions;

	@AfterEach
	void tearDown() {
		client.resources(Gitea.class).delete();
	}

	Gitea createDefault(String name) {
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

	@Test
	void createMinimalGitea() {
		Gitea gitea = createDefault("mygiteait");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(gitea.getSpec().getAdminPasswordLength()));
        });
	}

	@Test
	void createGiteaWithSso() {
		Gitea gitea = createDefault("mygiteait");
		gitea.getSpec().setSso(true);
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(240, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			assertThat(KeycloakDependentResource.getResource(gitea, client).get().getStatus().getReady(), is(true));
		});
	}

	@Test
	void createGiteaWithPassword() {
		Gitea gitea = createDefault("mygiteaitwpw");
		String password = "test1234";
		gitea.getSpec().setAdminPassword(password);
		gitea.getSpec().setAdminPasswordLength(password.length());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(gitea.getSpec().getAdminPassword()));
        });
	}

	@Test
	void changeGiteaPassword() {
		String changedPassword = "pwchange123"; // notsecret
		Gitea gitea = createDefault("pwchange");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		client.apps().deployments()
                .inNamespace(gitea.getMetadata().getNamespace())
                .withName(gitea.getMetadata().getName()).waitUntilCondition(c -> c != null && c.getStatus().getReadyReplicas() != null && c.getStatus().getReadyReplicas() == 1, 120, TimeUnit.SECONDS);
		await().atMost(30, TimeUnit.SECONDS).then().until(() -> client.resources(Gitea.class).withName("pwchange").edit(n -> {
			n.getSpec().setAdminPassword(changedPassword);
			return n;
			
		}) != null)
		;
		await().ignoreExceptions().atMost(150, TimeUnit.SECONDS).untilAsserted(() -> {
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(changedPassword));
			//Check if Admin can login
			//String host = GiteaRouteDependentResource.getResource(gitea, client).get().getStatus().getIngress().get(0).getHost();
			//AccessToken token = tokenService.createUserToken("http://" + host, gitea.getSpec().getAdminUser(), changedPassword);
			//assertThat(token,  is(IsNull.notNullValue()));
        });
	}
}
