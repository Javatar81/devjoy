package io.devjoy.gitea.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openapi.quarkus.gitea_json.model.Organization;
import org.openapi.quarkus.gitea_json.model.User;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.organization.domain.OrganizationVisibility;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationConditionType;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationSpec;
import io.devjoy.gitea.service.OrganizationService;
import io.devjoy.gitea.service.UserService;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@QuarkusTest
public class GiteaOrganizationReconcilerIT {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));

	@Inject
	OrganizationService orgService;
	@Inject
	UserService userService;
	
    @BeforeAll
	static void beforeAllTests() {
		Gitea gitea = createDefaultGitea("mygiteait-" + System.currentTimeMillis());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
	}

    @AfterAll
	static void afterAllTests() {
    	client.resources(Gitea.class).inNamespace(getTargetNamespace()).delete();
	}
    
    @AfterEach
	void tearDown() {
    	client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).delete();
	}

    @Test
    void createFullOrg() {
    	GiteaOrganization org = createDefaultOrg("myorg");
        client.resource(org).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	GiteaOrganization orgRes = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).get();
            assertThat(orgRes, is(IsNull.notNullValue()));
            assertThat(orgRes.getStatus().getConditions().stream().anyMatch(c -> GiteaOrganizationConditionType.GITEA_ORG_CREATED.toString().equals(c.getType())), is(true));
        });
    }
    
    @Test
    void createMultipleOrgs() {
    	GiteaOrganization[] orgs = new GiteaOrganization[3];
    	for (int i = 0; i < 3; i++) {
    		orgs[i] = createDefaultOrg("myorg" + i);
            client.resource(orgs[i]).create();
		}
    	
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	for (int i = 0; i < 3; i++) {
	        	GiteaOrganization orgRes = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(orgs[i].getMetadata().getName()).get();
	            assertThat(orgRes, is(IsNull.notNullValue()));
	            assertThat(orgRes.getStatus().getConditions().stream().anyMatch(c -> GiteaOrganizationConditionType.GITEA_ORG_CREATED.toString().equals(c.getType())), is(true));
        	}
        });
    }
    
    @Test
    void deleteFullOrg() {
    	GiteaOrganization org = createDefaultOrg("myorgdel");
        client.resource(org).create();
        client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).waitUntilReady(180, TimeUnit.SECONDS);
      
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	Optional<Gitea> gitea = org.associatedGitea(client);
        	assertThat(gitea.isPresent(), is(true));
        	
            client.resource(org).delete();
            Secret adminSecret = GiteaAdminSecretDependent.getResource(gitea.get(), client).get();
        	assertNotNull(adminSecret);
        	Optional<String> adminToken = GiteaAdminSecretDependent.getAdminToken(adminSecret);
        	assertThat(adminToken.isPresent(), is(true));
        	GiteaOrganization orgResAfterDeletion = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).get();
        	assertThat(orgResAfterDeletion, is(IsNull.nullValue()));
        	Optional<Organization> orgAfterDeletion = orgService.get(gitea.get(), "myorgdel", adminToken.get());
            assertThat(orgAfterDeletion.isEmpty(), is(true));
        });
    }
    
    @Test
    void updateFullOrg() {
    	GiteaOrganization org = createDefaultOrg("myupdate");
        client.resource(org).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	Optional<Gitea> gitea = org.associatedGitea(client);
        	assertThat(gitea.isPresent(), is(true));
        	Secret adminSecret = GiteaAdminSecretDependent.getResource(gitea.get(), client).get();
        	assertNotNull(adminSecret);
        	Optional<String> adminToken = GiteaAdminSecretDependent.getAdminToken(adminSecret);
        	assertThat(adminToken.isPresent(), is(true));
        	GiteaOrganization orgRes = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).get();
            client.resource(org).edit(o -> {
            	 org.getSpec().setDescription("update1");
                 org.getSpec().setLocation("update2");
                 org.getSpec().setWebsite("https://udpated.example.com");
                 return org;
            });
        	assertThat(orgRes, is(IsNull.notNullValue()));
        	Optional<User> owner = userService.getUser(gitea.get(), org.getSpec().getOwner(), adminToken.get());
        	assertThat(owner.isPresent(), is(true));
        	Optional<Organization> updatedOrg = orgService.get(gitea.get(), "myupdate", adminToken.get());        	
        	assertThat(updatedOrg.isPresent(), is(true));
        	assertThat("update1", is(updatedOrg.get().getDescription()));
        	assertThat("update2", is(updatedOrg.get().getLocation()));
        	assertThat("https://udpated.example.com", is(updatedOrg.get().getWebsite()));
            //assertThat(orgRes.getStatus().getConditions().stream().anyMatch(c -> GiteaOrganizationConditionType.GITEA_ORG_CREATED.toString().equals(c.getType())), is(true));
        });
    }
    
    @Test
    void createOrgInvalidWebsite() {
    	GiteaOrganization org = createDefaultOrg("myfaultyorg");
    	org.getSpec().setWebsite("novalidurl");
        client.resource(org).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	GiteaOrganization orgRes = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).get();
            assertThat(orgRes, is(IsNull.notNullValue()));
            assertThat(orgRes.getStatus().getConditions().stream().anyMatch(c -> GiteaOrganizationConditionType.GITEA_API_ERROR.toString().equals(c.getType())), is(true));
        });
    }
    
    GiteaOrganization createDefaultOrg(String name) {
        GiteaOrganization org = new GiteaOrganization();
        org.setMetadata(new ObjectMetaBuilder()
                .withName(name)
                .withNamespace(getTargetNamespace())
                .build()); 
        GiteaOrganizationSpec spec = new GiteaOrganizationSpec();
        spec.setOwner("testuser");
        spec.setVisibility(OrganizationVisibility.PUBLIC);
        spec.setDescription("This is a new org");
        spec.setLocation("Virtual");
        spec.setOwnerEmail("testuser@example.com");
        spec.setWebsite("https://example.com");
        org.setSpec(spec);
        return org;
    }
    
    static Gitea createDefaultGitea(String name) {
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
		spec.setLogLevel(GiteaLogLevel.DEBUG);
		spec.setAllowCreateOrganization(true);
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
