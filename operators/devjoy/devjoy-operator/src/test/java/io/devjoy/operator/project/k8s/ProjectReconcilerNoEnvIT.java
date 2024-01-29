package io.devjoy.operator.project.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

@QuarkusTest
public class ProjectReconcilerNoEnvIT {
    @Inject
	OpenShiftClient client;

    @AfterEach
	void tearDown() {
        client.resources(Project.class).delete();
	}

    @Test
    public void testCreateProject() {
        Project project = new Project();
        project.getMetadata().setNamespace(client.getNamespace());
        project.getMetadata().setName("noenvproj");
        ProjectSpec spec = new ProjectSpec();
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
        await().ignoreException(NullPointerException.class).atMost(200, TimeUnit.SECONDS).untilAsserted(() -> {
            final var projectResource = client.resources(Project.class).inNamespace(client.getNamespace()).withName(project.getMetadata().getName()).get();
            assertThat(projectResource.getStatus().getConditions().stream().anyMatch(c -> ProjectConditionType.ENV_NOT_FOUND.toString().equals(c.getType())), is(true));

        });
    }


}
