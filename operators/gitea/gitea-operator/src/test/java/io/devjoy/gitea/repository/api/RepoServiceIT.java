package io.devjoy.gitea.repository.api;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.eclipse.microprofile.rest.client.RestClientDefinitionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.openapi.quarkus.gitea_json.api.AdminApi;
import org.openapi.quarkus.gitea_json.model.CreateUserOption;
import org.openapi.quarkus.gitea_json.model.Repository;

import io.devjoy.gitea.k8s.TestEnvironment;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAssertions;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.repository.domain.Visibility;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.repository.k8s.model.GiteaRepositorySpec;
import io.devjoy.gitea.service.RepositoryService;
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
    
  
    //@Test
    void createRepo() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	Route route = GiteaRouteDependent.getResource(gitea, client).waitUntilCondition(r -> !r.getStatus().getIngress().isEmpty() && r.getStatus().getIngress().get(0).getHost() != null, 60, TimeUnit.SECONDS);
    	String baseUri = String.format("http://%s/api/v1", route.getStatus().getIngress().get(0).getHost());
    	RepositoryService repoService = RestClientBuilder.newBuilder().baseUri(new URI(baseUri)).build(RepositoryService.class);
    	await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
    		Secret adminSecret = GiteaAdminSecretDependent.getResource(gitea, client).get();
			assertNotNull(adminSecret);
			assertions.assertGiteaDeployment(gitea);
			String token = new String(java.util.Base64.getDecoder().decode(adminSecret.getData().get(GiteaAdminSecretDependent.DATA_KEY_TOKEN)));
			String auth = "token " + token;
			GiteaRepository repo = new GiteaRepository();
			repo.setMetadata(new ObjectMetaBuilder().withName("createRepoViaApi").build());
			GiteaRepositorySpec spec = new GiteaRepositorySpec();
			spec.setUser(gitea.getSpec().getAdminConfig().getAdminUser());
			spec.setVisibility(Visibility.PUBLIC);
			repo.setSpec(spec);
			Repository repository = repoService.create(repo, token, baseUri);
			assertNotNull(repository);
			repoService.delete(gitea.getSpec().getAdminConfig().getAdminUser(), repo.getMetadata().getName(), auth, baseUri);
			assertRepoDeleted(repoService, repo.getMetadata().getName(), auth, baseUri);
    	});
    }

	private void assertRepoDeleted(RepositoryService repoService, String repoName, String auth, String baseUri) {
		try {
			repoService.getByUserAndName(gitea.getSpec().getAdminConfig().getAdminUser(), repoName, auth, baseUri);
		} catch (WebApplicationException e) {
			assertEquals(404, e.getResponse().getStatus());
		}
	}
    
    //@Test
    void createRepoForOtherUser() throws IllegalStateException, RestClientDefinitionException, URISyntaxException {
    	Route route = GiteaRouteDependent.getResource(gitea, client).waitUntilCondition(r -> !r.getStatus().getIngress().isEmpty() && r.getStatus().getIngress().get(0).getHost() != null, 60, TimeUnit.SECONDS);
    	String baseUri = String.format("http://%s/api/v1", route.getStatus().getIngress().get(0).getHost());
    	RepositoryService repoService = getService(baseUri, RepositoryService.class);
    	AdminApi userService = getService(baseUri, AdminApi.class);
    	
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
    		Secret adminSecret = GiteaAdminSecretDependent.getResource(gitea, client).get();
    		assertNotNull(adminSecret);
			assertions.assertGiteaDeployment(gitea);
			String token = new String(java.util.Base64.getDecoder().decode(adminSecret.getData().get(GiteaAdminSecretDependent.DATA_KEY_TOKEN)));
			String auth = "token " + token;
			userService.adminCreateUser(createUser);
			GiteaRepository repo = new GiteaRepository();
			repo.setMetadata(new ObjectMetaBuilder().withName("createRepoViaUsr").build());
			GiteaRepositorySpec spec = new GiteaRepositorySpec();
			spec.setUser(username);
			spec.setVisibility(Visibility.PUBLIC);
			repo.setSpec(spec);
			Repository repository = repoService.create(repo, token, baseUri);
			assertNotNull(repository);
			repoService.delete(username, repo.getMetadata().getName(), auth, baseUri);
			assertRepoDeleted(repoService, repo.getMetadata().getName(), auth, baseUri);
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
		spec.getAdminConfig().setAdminUser("devjoyITAdmin");
		spec.getAdminConfig().setAdminEmail("devjoyITAdmin@example.com");
		spec.getAdminConfig().setAdminPassword(adminPassword);
		spec.setResourceRequirementsEnabled(false);
		spec.setLogLevel(GiteaLogLevel.DEBUG);
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
