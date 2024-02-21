package io.devjoy.gitea.repository.api;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.gitea_json.model.User;

import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.TestEnvironment;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAssertions;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositorySpec;
import io.devjoy.gitea.util.PasswordService;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;


@QuarkusTest
class UserServiceIT {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));
	static PasswordService pwService = new PasswordService();
	static String adminPassword = pwService.generateNewPassword(10);
	static String userPassword = pwService.generateNewPassword(10);
	static Gitea gitea;
	static GiteaAssertions assertions = new GiteaAssertions(client);
	@Inject
	UserService userService;
	
    @BeforeAll
	static void beforeAllTests() {
		gitea = createDefault("mygiteait-" + System.currentTimeMillis());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
	}

    @AfterAll
	static void tearDown() {
        client.resources(Gitea.class).inNamespace(getTargetNamespace()).delete();
	}
    
    private String getAdminToken(Gitea gitea) {
    	return new String(Base64.getDecoder().decode(GiteaAdminSecretDependentResource.getResource(gitea, client).get().getData().get(GiteaAdminSecretDependentResource.DATA_KEY_TOKEN)));
    }
    
    @Test
    void getUserNotExists() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
    		assertNotNull(GiteaAdminSecretDependentResource.getResource(gitea, client).get());
    		String adminToken = getAdminToken(gitea);
    		assertNotNull(adminToken);
    		assertTrue(adminToken.length() > 0);
    		assertions.assertGiteaDeployment(gitea);
    		try {
    			userService.getUser(gitea, "bvlsasfas", adminToken).isEmpty();
    		} catch (WebApplicationException e) {
    			assertEquals(e.getResponse().getStatus(), 404);
    		}
    	});	
    }
    
    @Test
    void createUser() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
    		assertNotNull(GiteaAdminSecretDependentResource.getResource(gitea, client).get());
    		String adminToken = getAdminToken(gitea);
    		assertNotNull(adminToken);
    		assertTrue(adminToken.length() > 0);
    		assertions.assertGiteaDeployment(gitea);
    		Optional<User> userCr = userService.createUser(gitea, "mytestusr", adminToken);
    		assertNotNull(userCr);
    		assertFalse(userCr.isEmpty());
        	Optional<User> userGet = userService.getUser(gitea, "mytestusr", adminToken);
        	assertNotNull(userGet);
        	assertFalse(userGet.isEmpty());
        	assertEquals(userCr.get().getId(), userGet.get().getId());
    	});
    }
    
    GiteaRepository createDefaultRepo(String name) {
        GiteaRepository repo = new GiteaRepository();
        repo.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(getTargetNamespace())
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
                .withNamespace(getTargetNamespace())
                .build()); 
		GiteaSpec spec = new GiteaSpec();
		spec.setAdminUser("devjoyITAdmin");
		spec.setAdminEmail("devjoyITAdmin@example.com");
		spec.setAdminPassword(adminPassword);
		spec.setResourceRequirementsEnabled(false);
		spec.setLogLevel(GiteaLogLevel.DEBUG);
		spec.setIngressEnabled(client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE));
		spec.setSso(false);
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		spec.setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		gitea.setSpec(spec);
		return gitea;
	}

    private static String getTargetNamespace() {
		return client.getNamespace() + "2";
	}
}

