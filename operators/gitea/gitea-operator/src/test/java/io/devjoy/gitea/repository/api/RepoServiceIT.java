package io.devjoy.gitea.repository.api;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.api.UserApi;
import org.openapi.quarkus.gitea_json.model.CreateRepoOption;
import org.openapi.quarkus.gitea_json.model.CreateUserOption;
import org.openapi.quarkus.gitea_json.model.GenerateRepoOption;
import org.openapi.quarkus.gitea_json.model.Repository;

import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.TestEnvironment;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAssertions;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependentResource;
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
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.ws.rs.WebApplicationException;


@QuarkusTest
class RepoServiceIT {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));
	static PasswordService pwService = new PasswordService();
	static String adminPassword = pwService.generateNewPassword(10);
	static String userPassword = pwService.generateNewPassword(10);
	static Gitea gitea;
	static GiteaAssertions assertions = new GiteaAssertions(client);
	
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
    
    @Test
    void createRepo() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	Route route = GiteaRouteDependentResource.getResource(gitea, client).waitUntilCondition(r -> !r.getStatus().getIngress().isEmpty() && r.getStatus().getIngress().get(0).getHost() != null, 60, TimeUnit.SECONDS);
    	String baseUri = String.format("http://%s/api/v1", route.getStatus().getIngress().get(0).getHost());
    	RepoService repoService = RestClientBuilder.newBuilder().baseUri(new URI(baseUri)).build(RepoService.class);
    	CreateRepoOption createRepo = new CreateRepoOption();
    	createRepo.setDefaultBranch("main");
    	createRepo.setName("createRepoViaApi");
    	await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
    		Secret adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client).get();
			assertNotNull(adminSecret);
			assertions.assertGiteaDeployment(gitea);
			String token = new String(java.util.Base64.getDecoder().decode(adminSecret.getData().get(GiteaAdminSecretDependentResource.DATA_KEY_TOKEN)));
			String auth = "token " + token;
			Repository repository = repoService.create(auth, createRepo);
			assertNotNull(repository);
			repoService.deleteByUserAndName(auth, gitea.getSpec().getAdminUser(), createRepo.getName());
			assertRepoDeleted(repoService, createRepo.getName(), auth);
    	});
    }

	private void assertRepoDeleted(RepoService repoService, String repoName, String auth) {
		try {
			repoService.getByUserAndName(auth, gitea.getSpec().getAdminUser(), repoName);
		} catch (WebApplicationException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}
    
    @Test
    void createRepoForOtherUser() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	Route route = GiteaRouteDependentResource.getResource(gitea, client).waitUntilCondition(r -> !r.getStatus().getIngress().isEmpty() && r.getStatus().getIngress().get(0).getHost() != null, 60, TimeUnit.SECONDS);
    	String baseUri = String.format("http://%s/api/v1", route.getStatus().getIngress().get(0).getHost());
    	RepoService repoService = getService(baseUri, RepoService.class);
    	AdminApi userService = getService(baseUri, AdminApi.class);
    	
    	CreateRepoOption createRepo = new CreateRepoOption();
    	createRepo.setDefaultBranch("main");
    	createRepo.setName("createRepoViaApi");
    	
    	String username = "test123";
    	CreateUserOption createUser = new CreateUserOption();
    	createUser.setLoginName(username);
    	createUser.setUsername(username);
    	createUser.setEmail(username + "@example.com");
    	createUser.setMustChangePassword(false);
    	createUser.setPassword(userPassword);
    	createUser.setFullName(username);
    	createUser.setMustChangePassword(false);
    	
    	await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
    		Secret adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client).get();
    		assertNotNull(adminSecret);
			assertions.assertGiteaDeployment(gitea);
			String token = new String(java.util.Base64.getDecoder().decode(adminSecret.getData().get(GiteaAdminSecretDependentResource.DATA_KEY_TOKEN)));
			String auth = "token " + token;
			userService.adminCreateUser(createUser);
			Repository repository = repoService.createRepositoryOnBehalf(auth, username, createRepo);
			assertNotNull(repository);
			repoService.deleteByUserAndName(auth, username, createRepo.getName());
			assertRepoDeleted(repoService, createRepo.getName(), auth);
    	});
    }
    
    private <T> T getService(String baseUri, Class<T> serviceClass) throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	return RestClientBuilder.newBuilder().baseUri(new URI(baseUri)).build(serviceClass);
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
