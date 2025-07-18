package io.devjoy.operator.project.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.DevEnvironmentSpec;
import io.devjoy.operator.environment.k8s.GiteaConfigSpec;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class ProjectReconcilerIT {
    
    static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);

    @AfterEach
	void tearDown() {
    	while (!client.resources(Project.class).inNamespace(getTargetNamespace()).list().getItems().isEmpty()) {    		
    		client.resources(Project.class).inNamespace(getTargetNamespace()).delete();
    	}
    	while (!client.resources(DevEnvironment.class).inNamespace(getTargetNamespace()).list().getItems().isEmpty()) {    
    		client.resources(DevEnvironment.class).inNamespace(getTargetNamespace()).delete();
    	}
	}

    @Test
    public void testCreateProject() {
        DevEnvironment devEnvironment = creaDevEnvironment();
        Project project = new Project();
        project.getMetadata().setNamespace(getTargetNamespace());
        project.getMetadata().setName("testproj");
        ProjectSpec spec = new ProjectSpec();
        spec.setEnvironmentName(devEnvironment.getMetadata().getName());
        spec.setEnvironmentNamespace(getTargetNamespace());
        ProjectOwner owner = new ProjectOwner();
        owner.setUser("testuser");
        owner.setUserEmail("testuser@example.com");
        spec.setOwner(owner);
        QuarkusSpec quarkusSpec = new QuarkusSpec();
        quarkusSpec.setEnabled(true);
        quarkusSpec.setExtensions(List.of("quarkus-rest"));
        // Needed to access the service via route, otherwise we get 401
        quarkusSpec.setExtensions(List.of("quarkus-smallrye-health"));
        spec.setQuarkus(quarkusSpec);
        project.setSpec(spec);
        client.resource(project).create();
        await().ignoreExceptionsMatching(e -> e instanceof NullPointerException || e instanceof UnknownHostException).atMost(600, TimeUnit.SECONDS).untilAsserted(() -> {
            final var projectResource = client.resources(Project.class).inNamespace(getTargetNamespace()).withName(project.getMetadata().getName()).get();
            assertNotNull(projectResource);
            assertThat(projectResource.getStatus(), is(IsNull.notNullValue()));
            assertThat(projectResource.getStatus().getWorkspace().getFactoryUrl(), is(IsNull.notNullValue()));
            Route route = client.routes().inNamespace(project.getMetadata().getNamespace()).withName(project.getMetadata().getName()).get();
            assertThat(route.getSpec(), is(IsNull.notNullValue()));
            assertThat(route.getSpec().getHost(), is(IsNull.notNullValue()));
            URI uri = new URI("http://" + route.getSpec().getHost() + "/q/health/live");
            var con = (HttpURLConnection) uri.toURL().openConnection();
            con.connect();
            assertThat(con.getResponseCode(), is(200));
        });
    }

    DevEnvironment creaDevEnvironment() {
        DevEnvironment env = new DevEnvironment();
        env.getMetadata().setName("test-env");
        env.getMetadata().setNamespace(getTargetNamespace());
        DevEnvironmentSpec spec = new DevEnvironmentSpec();
        spec.setMavenSettingsPvc("maven");
        GiteaConfigSpec giteaSpec = new GiteaConfigSpec();
        giteaSpec.setEnabled(true);
        giteaSpec.setManaged(true);
        spec.setGitea(giteaSpec);
        env.setSpec(spec);
        client.resource(env).create();
        return env;
    }

    private static String getTargetNamespace() {
		return client.getNamespace() + "2";
	}
}
