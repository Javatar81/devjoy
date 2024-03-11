package io.devjoy.operator.environment.k8s;

import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaDeploymentDependent;
import io.devjoy.operator.environment.k8s.deploy.ArgoCDDependentResource;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class DevEnvironmentReconcilerIT {
    
    static OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);

    @AfterEach
	void tearDown() {
		client.resources(DevEnvironment.class).inNamespace(getTargetNamespace()).delete();
	}

    @Test
    public void createMinimalEnvironment() {
        DevEnvironment env = new DevEnvironment();
        env.getMetadata().setName("test-env");
        env.getMetadata().setNamespace(getTargetNamespace());
        DevEnvironmentSpec spec = new DevEnvironmentSpec();
        GiteaConfigSpec giteaSpec = new GiteaConfigSpec();
        giteaSpec.setEnabled(true);
        giteaSpec.setManaged(true);
        spec.setGitea(giteaSpec);
        env.setSpec(spec);
        client.resource(env).create();
        await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
            
            final var giteaDeployment = GiteaDeploymentDependent.getResource(GiteaDependentResource.getResource(client, env).get(), client).get();
            assertThat(giteaDeployment, is(IsNull.notNullValue()));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
            assertThat(giteaDeployment.getStatus().getReadyReplicas(), is(1));
            
            final var argoCD = ArgoCDDependentResource.getResource(env, client).get();
            assertThat(argoCD, is(IsNull.notNullValue()));
            assertThat(argoCD.getStatus().getPhase(), is("Available"));
        });
    }

    private static String getTargetNamespace() {
		return client.getNamespace() + "2";
	}
    
}
