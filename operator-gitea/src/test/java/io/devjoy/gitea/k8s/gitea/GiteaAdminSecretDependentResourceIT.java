package io.devjoy.gitea.k8s.gitea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.devjoy.gitea.domain.AuthenticationService;
import io.devjoy.gitea.domain.GiteaApiService;
import io.devjoy.gitea.domain.GiteaPodExecService;
import io.devjoy.gitea.domain.PasswordService;
import io.devjoy.gitea.domain.UserService;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaReconciler;
import io.devjoy.gitea.k8s.GiteaSpec;
import io.devjoy.gitea.k8s.GiteaStatusUpdater;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

class GiteaAdminSecretDependentResourceIT {
	@Inject
	OpenShiftClient ocpClient;
	@Inject
	PasswordService passwordService;
	@Inject
	GiteaPodExecService execService;
	@Inject
	UserService userService;
	@Inject
	AuthenticationService authService;
	@Inject
	GiteaApiService giteaApiService;
	@Inject
	GiteaStatusUpdater updater;
	
	@RegisterExtension
	LocallyRunOperatorExtension operator = LocallyRunOperatorExtension.builder().withReconciler(new GiteaReconciler(ocpClient, userService, authService, updater))
			.build();
	@Test
	void updatesSubResourceStatus() {
		Gitea resource = new Gitea();
		resource.setMetadata(new ObjectMetaBuilder()
		        .withName("mygiteait")
		        .withNamespace(operator.getNamespace())
		        .build());
		GiteaSpec spec = new GiteaSpec();
		spec.setAdminUser("devjoyITAdmin");
		spec.setAdminEmail("devjoyITAdmin@example.com");
		resource.setSpec(spec);
		operator.create(resource);
		awaitStatusUpdated(resource.getMetadata().getName());
		System.out.println("Hello");
	}

	void awaitStatusUpdated(String name) {
		await("cr status updated").atMost(360, TimeUnit.SECONDS).untilAsserted(() -> {
			Gitea cr = operator.get(Gitea.class, name);
			assertThat(cr).isNotNull();
			assertThat(cr.getStatus()).isNotNull();

		});
	}
}
