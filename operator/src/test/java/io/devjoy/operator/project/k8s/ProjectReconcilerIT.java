package io.devjoy.operator.project.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

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
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ProjectReconcilerIT {
    @Inject
	OpenShiftClient client;

    @AfterEach
	void tearDown() {
		client.resources(DevEnvironment.class).delete();
        client.resources(Project.class).delete();
	}

    @Test
    public void testCreateProject() {
        DevEnvironment devEnvironment = creaDevEnvironment();
        Project project = new Project();
        project.getMetadata().setNamespace(client.getNamespace());
        project.getMetadata().setName("testproj");
        ProjectSpec spec = new ProjectSpec();
        spec.setEnvironmentName(devEnvironment.getMetadata().getName());
        spec.setEnvironmentNamespace(client.getNamespace());
        ProjectOwner owner = new ProjectOwner();
        owner.setUser("testuser");
        owner.setUserEmail("testuser@example.com");
        spec.setOwner(owner);
        QuarkusSpec quarkusSpec = new QuarkusSpec();
        quarkusSpec.setEnabled(true);
        quarkusSpec.setExtensions(List.of("quarkus-resteasy-reactive-jackson", "quarkus-jdbc-postgresql"));
        spec.setQuarkus(quarkusSpec);
        project.setSpec(spec);
        client.resource(project).create();
        await().ignoreException(NullPointerException.class).ignoreException(UnknownHostException.class).atMost(400, TimeUnit.SECONDS).untilAsserted(() -> {
            final var projectResource = client.resources(Project.class).inNamespace(client.getNamespace()).withName(project.getMetadata().getName()).get();
            assertThat(projectResource.getStatus().getWorkspace().getFactoryUrl(), is(IsNull.notNullValue()));
            Ingress ingress = client.network().v1().ingresses().inNamespace(project.getMetadata().getNamespace()).withName(project.getMetadata().getName()).get();
            assertThat(ingress, is(IsNull.notNullValue()));
            URI uri = new URI("http://" + ingress.getSpec().getRules().get(0).getHost());
            var con = (HttpURLConnection) uri.toURL().openConnection();
            con.connect();
            assertThat(con.getResponseCode(), is(200));
        });
    }

    DevEnvironment creaDevEnvironment() {
        DevEnvironment env = new DevEnvironment();
        env.getMetadata().setName("test-env");
        env.getMetadata().setNamespace(client.getNamespace());
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
}
