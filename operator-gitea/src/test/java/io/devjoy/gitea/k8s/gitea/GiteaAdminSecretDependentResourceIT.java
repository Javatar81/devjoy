package io.devjoy.gitea.k8s.gitea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.TimeUnit;

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
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestExtension;
import jakarta.inject.Inject;

//We need this for dependency injection such as OpenShift Client 
//But there is an issue https://github.com/quarkiverse/quarkus-operator-sdk/issues/328
//@QuarkusTest
class GiteaAdminSecretDependentResourceIT {
	
	/*@Inject
	private OpenShiftClient ocpClient;
	@Inject
	private UserService userService;
	@Inject
	private GiteaStatusUpdater updater;
	@Inject
	private AuthenticationService authService;*/

	OpenShiftClient ocpClient = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	
	PasswordService passwordService = new PasswordService();
	
	GiteaPodExecService execService = new GiteaPodExecService(ocpClient);
	
	UserService userService = new UserService(passwordService, execService);
	
	AuthenticationService authService = new AuthenticationService(ocpClient, execService);
	
	GiteaApiService giteaApiService = new GiteaApiService(ocpClient);
	
	GiteaStatusUpdater updater = new GiteaStatusUpdater();
	
	GiteaReconciler reconciler = new GiteaReconciler(ocpClient, userService, authService, updater);

	@RegisterExtension
	LocallyRunOperatorExtension operator = buildOperator();

	protected LocallyRunOperatorExtension buildOperator() {
		return LocallyRunOperatorExtension.builder().withKubernetesClient(ocpClient).withReconciler(new GiteaReconciler(ocpClient, userService, authService, updater))
			.build();
		}
	@Test
	void updatesSubResourceStatus() {
		
		Gitea resource = new Gitea();
		resource.setMetadata(new ObjectMetaBuilder()
		        .withName("mygiteait")
		        .withNamespace(operator.getNamespace())
		        .build());
		GiteaSpec spec = new GiteaSpec();
		System.out.println("Operator Namespace: " + operator.getNamespace());
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
