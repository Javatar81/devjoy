package io.devjoy.gitea.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.organization.domain.OrganizationVisibility;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationConditionType;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationSpec;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GiteaOrganizationReconcilerIT {

	static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	static TestEnvironment env = new TestEnvironment(client, ConfigProvider.getConfig().getOptionalValue("test.quarkus.kubernetes-client.devservices.flavor", String.class));

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
    void updateFullOrg() {
    	GiteaOrganization org = createDefaultOrg("myupdate");
        client.resource(org).create();
       
       
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
        	GiteaOrganization orgRes = client.resources(GiteaOrganization.class).inNamespace(getTargetNamespace()).withName(org.getMetadata().getName()).get();
        	 //NEED THE RESOURCE here
            client.resource(org).edit(o -> {
            	 org.getSpec().setDescription("update1");
                 org.getSpec().setLocation("update2");
                 org.getSpec().setWebsite("https://udpated.example.com");
                 return org;
            });
        	assertThat(orgRes, is(IsNull.notNullValue()));
           
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
