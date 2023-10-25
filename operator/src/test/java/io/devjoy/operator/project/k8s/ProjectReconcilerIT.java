package io.devjoy.operator.project.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.environment.k8s.DevEnvironmentSpec;
import io.devjoy.operator.environment.k8s.GiteaConfigSpec;
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
        project.getMetadata().setName("testproject");
        ProjectSpec spec = new ProjectSpec();
        spec.setEnvironmentName(devEnvironment.getMetadata().getName());
        spec.setEnvironmentNamespace(devEnvironment.getMetadata().getNamespace());
        ProjectOwner owner = new ProjectOwner();
        owner.setUser("testuser");
        owner.setUserEmail("testuser@example.com");
        spec.setOwner(owner);
        QuarkusSpec quarkusSpec = new QuarkusSpec();
        quarkusSpec.setEnabled(true);
        quarkusSpec.setExtensions(List.of("quarkus-resteasy-reactive-jackson"));
        spec.setQuarkus(quarkusSpec);
        project.setSpec(spec);
        client.resource(project).create();
        await().ignoreException(NullPointerException.class).atMost(300, TimeUnit.SECONDS).untilAsserted(() -> {
            final var projectResource = client.resources(Project.class).inNamespace(client.getNamespace()).withName(project.getMetadata().getName()).get();
            assertThat(projectResource.getStatus().getWorkspace().getFactoryUrl(), is(IsNull.notNullValue()));
        });
    }

    DevEnvironment creaDevEnvironment() {
        DevEnvironment env = new DevEnvironment();
        env.getMetadata().setName("test-env");
        env.getMetadata().setNamespace(client.getNamespace());
        DevEnvironmentSpec spec = new DevEnvironmentSpec();
        GiteaConfigSpec giteaSpec = new GiteaConfigSpec();
        giteaSpec.setEnabled(true);
        giteaSpec.setManaged(true);
        spec.setGitea(giteaSpec);
        env.setSpec(spec);
        client.resource(env).create();
        return env;
    }
}