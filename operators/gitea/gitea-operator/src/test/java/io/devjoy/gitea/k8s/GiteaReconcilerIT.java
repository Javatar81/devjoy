package io.devjoy.gitea.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.k8s.v2alpha1.Keycloak;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImport;
import org.keycloak.k8s.v2alpha1.KeycloakRealmImportSpec;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.Placeholders;
import org.keycloak.k8s.v2alpha1.keycloakrealmimportspec.Realm;
import org.keycloak.v1alpha1.KeycloakClient;
import org.keycloak.v1alpha1.KeycloakRealm;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAssertions;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependent;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependent;
import io.devjoy.gitea.k8s.keycloak.KeycloakService;
import io.devjoy.gitea.k8s.keycloak.SsoService;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConfigOverrides;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.k8s.model.keycloak.KeycloakSpec;
import io.devjoy.gitea.k8s.model.keycloak.KeycloakUnmanagedConfig;
import io.devjoy.gitea.k8s.model.postgres.PostgresUnmanagedConfig;
import io.devjoy.gitea.service.GiteaApiService;
import io.devjoy.gitea.service.UserService;
import io.devjoy.gitea.util.ApiAccessMode;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GiteaReconcilerIT {
	
	OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	ApiAccessMode accessMode = ConfigProviderResolver.instance().getConfig().getValue("io.devjoy.gitea.api.access.mode", ApiAccessMode.class);
	String fallback = ConfigProviderResolver.instance().getConfig().getValue("io.devjoy.gitea.api.access.fallback", String.class);
	String pgImageK8s = ConfigProviderResolver.instance().getConfig().getValue("io.devjoy.gitea.postgres.image.k8s", String.class);
	TestEnvironment env = new TestEnvironment(client);
	GiteaApiService apiService = new GiteaApiService(client, accessMode, fallback);
	UserService userService = new UserService(apiService);
	KeycloakService keycloakK8sService = new KeycloakService(client, getTargetNamespace());
	SsoService ssoService = new SsoService(client, getTargetNamespace());
	GiteaAssertions assertions = new GiteaAssertions(client);
	static GiteaPrereqs prereqs = new GiteaPrereqs();

	@BeforeAll
	static void setUp() {
		prereqs.assureKeycloakCrdsInstalled();
	}

	@AfterEach
	void tearDown() {
		client.resources(Gitea.class).inNamespace(getTargetNamespace()).delete();
	}
	
	public GiteaReconcilerIT() {
		apiService.setAccessMode(accessMode);
	}

	Gitea createDefault(String name) {
		Gitea gitea = new Gitea();
				gitea.setMetadata(new ObjectMetaBuilder()
						.withName(name)
						.withNamespace(getTargetNamespace())
						.build()); 
		GiteaSpec spec = new GiteaSpec();
		spec.getAdminConfig().setAdminUser("devjoyITAdmin");
		spec.getAdminConfig().setAdminEmail("devjoyITAdmin@example.com");
		spec.setResourceRequirementsEnabled(false);
		spec.setIngressEnabled(true);
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().getManagedConfig().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		spec.setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
		gitea.setSpec(spec);
		return gitea;
	}

	private String getTargetNamespace() {
		return client.getNamespace() + "2";
	}

	@Test
	void createGiteaWithoutSpec() {
		Gitea gitea = new Gitea();
		gitea.setMetadata(new ObjectMetaBuilder()
						.withName("giteawithoutspec")
						.withNamespace(getTargetNamespace())
						.build()); 
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
			// check that we create the deployment
			// Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependent.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(10));
		});
	}

	@Test
	void createMinimalGitea() {
		Gitea gitea = createDefault("mygiteait");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
			// check that we create the deployment
			// Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependent.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(gitea.getSpec().getAdminConfig().getAdminPasswordLength()));
		});
	}

	@Test
	void createWithUnmanagedPostgres() {
		try {
			Gitea gitea = createDefault("mygiteaitpgunmgt");
			PostgresUnmanagedConfig spec = new PostgresUnmanagedConfig();
			spec.setExtraSecretName("postgres-secret");
			//pgpw12345
			spec.setDatabaseName("pgtestdb");
			spec.setHostName("pgtest");
			spec.setUserName("pguser");
			gitea.getSpec().getPostgres().setManaged(false);
			gitea.getSpec().getPostgres().setUnmanagedConfig(spec);
			env.createStaticPVsIfRequired();

			deployExtraPostgres(gitea, client);
			await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
				// check that we create the deployment
				// Postgres PVC
				assertions.assertGiteaPvc(gitea);
				assertions.assertGiteaDeployment(gitea);
				assertions.assertAdminSecret(gitea);
				final var adminSecret = GiteaAdminSecretDependent.getResource(gitea, client);
				assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(gitea.getSpec().getAdminConfig().getAdminPasswordLength()));
			});
		} finally {
			client.resources(Service.class).inNamespace(getTargetNamespace()).withName("pgtest").delete();
			client.resources(Deployment.class).inNamespace(getTargetNamespace()).withName("pgtest").delete();
			client.resources(Secret.class).inNamespace(getTargetNamespace()).withName("postgres-secret").delete();
		}
		
	}

	private void deployExtraPostgres(Gitea gitea, KubernetesClient client) {
		Deployment pgDeployment = client.apps().deployments()
				.load(getClass().getClassLoader().getResourceAsStream("postgres/deployment.yaml"))
				.item();
		pgDeployment.getMetadata().setNamespace(getTargetNamespace());
		if(!OpenShiftActivationCondition.serverSupportsApi(client)) {
			pgDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).setImage(pgImageK8s);
		}
		client.resource(pgDeployment).create();
		Service pgService = client.services()
				.load(getClass().getClassLoader().getResourceAsStream("postgres/service.yaml"))
				.item();
		pgService.getMetadata().setNamespace(getTargetNamespace());
		client.resource(pgService).create();
		Secret extraSecret = client.secrets()
				.load(getClass().getClassLoader().getResourceAsStream("postgres/secret.yaml"))
				.item();
				extraSecret.getMetadata().setNamespace(getTargetNamespace());
		client.resource(extraSecret).create();
		client.resource(gitea).create();
	}

	@Test
	void createGiteaWithSso() {
		Gitea gitea = createDefault("mygiteait");
		KeycloakSpec keycloak = new KeycloakSpec();
		keycloak.setEnabled(true);
		keycloak.setManaged(true);
		gitea.getSpec().setKeycloak(keycloak);
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(400, TimeUnit.SECONDS).untilAsserted(() -> {
			// check that we create the deployment
			// Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			assertThat(KeycloakDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
			assertThat(KeycloakRealmDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
			assertThat(KeycloakClientDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
		});
	}

	@Test
	void createGiteaWithUnmanagedRhsso() throws IOException {
		String clientSecret = "dshsdhdfjdh634634";
		String oidcSecretName = "oidc-secret";
		String oidcSecretKey = "secret";
		String oidcClient = "mygiteait-devjoy-gitea";
		Secret oidcSecret = new SecretBuilder()
			.withNewMetadata().withNamespace(getTargetNamespace()).withName(oidcSecretName).endMetadata()
			.addToStringData(oidcSecretKey, clientSecret)
			.build();
		
		org.keycloak.v1alpha1.Keycloak keycloak = null;
		KeycloakRealm keycloakRealm = null;
		KeycloakClient keycloakClient= null;
		try
		{
			
			Gitea gitea = createDefault("mygiteait");
			KeycloakSpec keycloakSpec = new KeycloakSpec();
			keycloakSpec.setEnabled(true);
			keycloakSpec.setManaged(false);
			KeycloakUnmanagedConfig config = new KeycloakUnmanagedConfig();
			config.setOidcClient("mygiteait-devjoy-gitea");
			config.setOidcAutoDiscoverUrl(ssoService.getHostname() + "/auth/realms/mygiteait-devjoy/.well-known/openid-configuration");
			config.setOidcExtraSecretName(oidcSecretName);
			keycloakSpec.setUnmanagedConfig(config);
			gitea.getSpec().setKeycloak(keycloakSpec);
			env.createStaticPVsIfRequired();
			keycloak = ssoService.newKeycloak();
			keycloakRealm = ssoService.newKeycloakRealm();
			keycloakClient = ssoService.newKeycloakClient("mygiteait", oidcClient, clientSecret);
			// Create the resources
			client.resource(keycloak).create();
			client.resource(oidcSecret).create();
			client.resource(keycloakClient).create();
			client.resource(keycloakRealm).create();
			client.resource(gitea).create();
			await().ignoreException(NullPointerException.class).atMost(400, TimeUnit.SECONDS).untilAsserted(() -> {
				// check that we create the deployment
				// Postgres PVC
				assertions.assertPostgresPvc(gitea);
				assertions.assertGiteaPvc(gitea);
				assertions.assertGiteaDeployment(gitea);
				assertions.assertAdminSecret(gitea);
				assertThat(KeycloakDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
				assertThat(KeycloakRealmDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
				assertThat(KeycloakClientDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
			});
		} finally{
			if (client.resource(oidcSecret).get() != null) {
				client.resource(oidcSecret).delete();
			}
			if (keycloak != null && client.resource(keycloak).get() != null) {
				client.resource(keycloak).delete();
			}
			if (keycloakRealm != null && client.resource(keycloakRealm).get() != null) {
				client.resource(keycloakRealm).delete();
			}
			if (keycloakClient != null && client.resource(keycloakClient).get() != null) {
				client.resource(keycloakClient).delete();
			}
		}
	}


	//TODO Trust problem with self-signed cert
	void createGiteaWithUnmanagedKeycloak() throws IOException {
		String clientSecret = "dshsdhdfjdh634634";
		String oidcSecretName = "oidc-secret";
		String oidcSecretKey = "secret";
		Secret oidcSecret = new SecretBuilder()
			.withNewMetadata().withNamespace(getTargetNamespace()).withName(oidcSecretName).endMetadata()
			.addToStringData(oidcSecretKey, clientSecret)
			.build();
		Secret tlsSecret = null;
		KeycloakRealmImport keycloakRealmImport = null;
		Keycloak keycloak = null;
		try
		{
			tlsSecret = keycloakK8sService.newTlsSecret();
			Gitea gitea = createDefault("mygiteait");
			//TODO /etc/pki/ca-trust/extracted/keycloak
			KeycloakSpec keycloakSpec = new KeycloakSpec();
			keycloakSpec.setEnabled(true);
			keycloakSpec.setManaged(false);
			KeycloakUnmanagedConfig config = new KeycloakUnmanagedConfig();
			config.setOidcClient("mygiteait-devjoy-gitea");
			config.setOidcAutoDiscoverUrl(keycloakK8sService.getHostname() + "/realms/mygiteait-devjoy/.well-known/openid-configuration");
			//config.setOidcAutoDiscoverUrl(String.format("https://example-keycloak-service.%s.svc.cluster.local:8443", getTargetNamespace()) + "/realms/mygiteait-devjoy/.well-known/openid-configuration");
			config.setOidcExtraSecretName(oidcSecretName);
			keycloakSpec.setUnmanagedConfig(config);
			gitea.getSpec().setKeycloak(keycloakSpec);
			env.createStaticPVsIfRequired();
			keycloak = keycloakK8sService.newKeycloak();
			keycloakRealmImport = keycloakK8sService.newRealmImport(oidcSecretName, oidcSecretKey);

			// Create the resources
			client.resource(keycloakK8sService.newKeycloak()).create();
			client.resource(oidcSecret).create();
			client.resource(tlsSecret).create();
			client.resource(keycloakRealmImport).create();
			client.resource(gitea).create();
			await().ignoreException(NullPointerException.class).atMost(400, TimeUnit.SECONDS).untilAsserted(() -> {
				// check that we create the deployment
				// Postgres PVC
				assertions.assertPostgresPvc(gitea);
				assertions.assertGiteaPvc(gitea);
				assertions.assertGiteaDeployment(gitea);
				assertions.assertAdminSecret(gitea);
				assertThat(KeycloakDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
				assertThat(KeycloakRealmDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
				assertThat(KeycloakClientDependent.getResource(gitea, client).get().getStatus().getReady(), is(true));
			});
		} finally{
			if (client.resource(oidcSecret).get() != null) {
				client.resource(oidcSecret).delete();
			}
			if (keycloak != null && client.resource(keycloak).get() != null) {
				client.resource(keycloak).delete();
			}
			if (keycloakRealmImport != null && client.resource(keycloakRealmImport).get() != null) {
				client.resource(keycloakRealmImport).delete();
			}
			if (tlsSecret != null && client.resource(tlsSecret).get() != null) {
				client.resource(tlsSecret).delete();
			}
			
		}
	}

	@Test
	void createGiteaWithPassword() {
		Gitea gitea = createDefault("mygiteaitwpw");
		String password = "test1234";
		gitea.getSpec().getAdminConfig().setAdminPassword(password);
		gitea.getSpec().getAdminConfig().setAdminPasswordLength(password.length());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependent.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(gitea.getSpec().getAdminConfig().getAdminPassword()));
        });
	}

	@Test
	void createGiteaWithExistingAdminSecret() {
		String password = "test1234";
		Secret adminSecret = new SecretBuilder()
			.withNewMetadata().withNamespace(getTargetNamespace()).withName("admin-extra-pw-secret").endMetadata()
				.addToStringData("password", password)
			.build();
		try {
			client.resource(adminSecret).create();
			client.resource(adminSecret).waitUntilReady(20, TimeUnit.SECONDS);
			Gitea gitea = createDefault("mygiteaitwpw");
			gitea.getSpec().getAdminConfig().setExtraAdminSecretName("admin-extra-pw-secret");
			env.createStaticPVsIfRequired();
			client.resource(gitea).create();
			await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
				assertions.assertPostgresPvc(gitea);
				assertions.assertGiteaPvc(gitea);
				assertions.assertGiteaDeployment(gitea);
				assertions.assertAdminSecret(gitea);
				final var adminSecretUpdated = GiteaAdminSecretDependent.getResource(gitea, client);
				assertThat(new String(java.util.Base64.getDecoder().decode(adminSecretUpdated.get().getData().get("password"))), is(password));
			});
		} finally {
			if (client.resource(adminSecret).get() != null) {
				client.resource(adminSecret).delete();
			}
		}
	}

	@Test
	void changeExistingAdminSecret() {
		String password = "test1234";
		String changedPassword = "12333555sdg";
		Secret adminSecret = new SecretBuilder()
			.withNewMetadata().withNamespace(getTargetNamespace()).withName("admin-extra-pw-secret").endMetadata()
				.addToStringData("password", password)
			.build();
		try {
			Gitea gitea = createDefault("mygiteaitwpw");
			client.resource(adminSecret).create();
			client.resource(adminSecret).waitUntilReady(20, TimeUnit.SECONDS);
			gitea.getSpec().getAdminConfig().setExtraAdminSecretName("admin-extra-pw-secret");
			env.createStaticPVsIfRequired();
			client.resource(gitea).create();
			client.apps().deployments()
                .inNamespace(getTargetNamespace())
                .withName(gitea.getMetadata().getName()).waitUntilCondition(c -> c != null && c.getStatus().getReadyReplicas() != null && c.getStatus().getReadyReplicas() == 1, 180, TimeUnit.SECONDS);
			

			
			await().atMost(30, TimeUnit.SECONDS).then().until(() -> 
				Optional.ofNullable(GiteaAdminSecretDependent.getResource(gitea, client).get()).flatMap(GiteaAdminSecretDependent::getAdminToken).isPresent()
				&& client.secrets().inNamespace(getTargetNamespace()).withName("admin-extra-pw-secret").edit(n -> {
					n.getStringData().put("password", changedPassword);
					return n;
					
				}) != null
			) 
			;
			await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
				assertions.assertPostgresPvc(gitea);
				assertions.assertGiteaPvc(gitea);
				assertions.assertGiteaDeployment(gitea);
				assertions.assertAdminSecret(gitea);
				final var adminSecretUpdated = GiteaAdminSecretDependent.getResource(gitea, client);
				assertThat(new String(java.util.Base64.getDecoder().decode(adminSecretUpdated.get().getData().get("password"))), is(changedPassword));
			});
		} finally {
			if (client.resource(adminSecret).get() != null) {
				client.resource(adminSecret).delete();
			}
		}
	}

	@Test
	void changeGiteaPassword() {
		String changedPassword = "pwchange123"; // notsecret
		Gitea gitea = createDefault("pwchange");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		client.apps().deployments()
                .inNamespace(getTargetNamespace())
                .withName(gitea.getMetadata().getName()).waitUntilCondition(c -> c != null && c.getStatus().getReadyReplicas() != null && c.getStatus().getReadyReplicas() == 1, 180, TimeUnit.SECONDS);
		await().atMost(30, TimeUnit.SECONDS).then().until(() -> client.resources(Gitea.class).inNamespace(getTargetNamespace()).withName("pwchange").edit(n -> {
			n.getSpec().getAdminConfig().setAdminPassword(changedPassword);
			return n;
			
		}) != null)
		;
		await().ignoreException(NullPointerException.class).atMost(150, TimeUnit.SECONDS).untilAsserted(() -> {
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependent.getResource(gitea, client).get();
			assertThat(adminSecret, is(IsNull.notNullValue()));
			assertThat(GiteaAdminSecretDependent.getAdminPassword(adminSecret).get(), is(changedPassword));
			assertThat(userService.createAccessToken(gitea, gitea.getSpec().getAdminConfig().getAdminUser(), changedPassword, "testpwchg", UserService.SCOPE_WRITE_REPO).isEmpty(), is(false));
        });
	}

	@Test
	void createGiteaWitAllFields() {
		Gitea gitea = createDefault("allfields");
		GiteaSpec spec = gitea.getSpec();
		KeycloakSpec keycloak = new KeycloakSpec();
		keycloak.setEnabled(true);
		keycloak.setManaged(true);
		spec.setKeycloak(keycloak);
		spec.getAdminConfig().setAdminPassword("test12345"); // notsecret
		spec.setAllowCreateOrganization(true);
		spec.setCpuLimit("750m");
		spec.setCpuRequest("250m");
		spec.setMemoryLimit("4Gi");
		spec.setMemoryRequest("256Mi");
		spec.setDisableRegistration(true);
		spec.setEnableCaptcha(true);
		spec.setImage("quay.io/rhpds/gitea");
		spec.setImageTag("1.21");
		spec.setLogLevel(GiteaLogLevel.INFO);
		spec.setRegisterEmailConfirm(false);
		spec.setResourceRequirementsEnabled(true);
		spec.setSsl(true);
		spec.setVolumeSize("2Gi");
		if (OpenShiftActivationCondition.serverSupportsApi(client)) {
			String baseDomain = client.config().ingresses().withName("cluster").get().getSpec().getDomain();
			spec.setRoute(String.format("%s-%s.%s", "gitea", getTargetNamespace(), baseDomain));
		}
		spec.getMailer().setEnabled(true);
		spec.getMailer().setFrom("gitea-devjoy@example.com");
		spec.getMailer().setHeloHostname("gitea");
		spec.getMailer().setHost("example.com");
		spec.getMailer().setPassword("test12345"); // notsecretnull);
		spec.getMailer().setProtocol("smtp");
		spec.getMailer().setUser("giteadm");
		spec.getPostgres().getManagedConfig().setMemoryLimit("1Gi");
		spec.getPostgres().getManagedConfig().setMemoryRequest("256Mi");
		spec.getPostgres().getManagedConfig().setCpuLimit("800m");
		spec.getPostgres().getManagedConfig().setCpuRequest("250m");
		spec.getPostgres().getManagedConfig().setImage("registry.redhat.io/rhel8/postgresql-12");
		spec.getPostgres().getManagedConfig().setImageTag("latest");
		spec.getPostgres().getManagedConfig().setVolumeSize("8Gi");
		spec.getPostgres().getManagedConfig().setSsl(true);
		//TODO Overrides
		GiteaConfigOverrides over = spec.getConfigOverrides();
		over.getActions().put("ENABLED", "true");
		over.getAdmin().put("DEFAULT_EMAIL_NOTIFICATIONS", "onmention");
		over.getApi().put("MAX_RESPONSE_ITEMS", "20");
		over.getAttachment().put("MAX_SIZE", "8");
		over.getCache().put("INTERVAL", "120");
		over.getCacheLastCommit().put("ITEM_TTL", "-1");
		over.getCamo().put("SERVER_URL", "example.com");
		over.getCors().put("ENABLED","true");
		over.getCron().put("ENABLED", "true");
		over.getCronArchiveCleanup().put("ENABLED", "false");
		over.getCronCheckRepoStats().put("RUN_AT_START", "false");
		over.getCronCleanupHookTaskTable().put("ENABLED", "false");
		over.getCronCleanupPackages().put("ENABLED", "false");
		over.getCronRepoHealthCheck().put("TIMEOUT", "45s");
		over.getCronSyncExternalUsers().put("UPDATE_EXISTING", "false");
		over.getCronUpdateMigrationPosterId().put("SCHEDULE", "@midnight");
		over.getCronUpdateMirrors().put("PULL_LIMIT", "30");
		over.getDatabase().put("DB_RETRIES","11");
		over.getDefaults().put("APP_NAME","Gitea: Git with a cup of tea under test");
		over.getEmailIncoming().put("USERNAME","devjoyusr");
		over.getFederation().put("MAX_SIZE","5");
		over.getGit().put("DISABLE_PARTIAL_CLONE","true");
		//TODO Leads to https://github.com/Javatar81/devjoy/issues/22 over.getGitConfig().put("core\\.logAllRefUpdates","false");
		over.getGitTimeout().put("DEFAULT", "400");
		over.getHighlightMapping().put("file_extension", "TOML");
		over.getI18n().put("LANGS", "en-US,de-DE,fr-FR");
		over.getI18n().put("NAMES", "English,Deutsch,Français");
		over.getIndexer().put("REPO_INDEXER_TYPE", "elasticsearch");
		over.getLfs().put("MINIO_INSECURE_SKIP_VERIFY", "true");
		over.getLog().put("ENABLE_SSH_LOG", "true");
		over.getLogConn().put("RECONNECT", "true");
		over.getLogConsole().put("STDERR", "true");
		over.getLogFile().put("FILE_NAME", "gitea-devjoy.log");
		over.getMailer().put("SUBJECT_PREFIX", "devjoy:");
		over.getMarkdown().put("ENABLE_MATH", "false");
		over.getMetrics().put("ENABLED", "true");
		over.getMigrations().put("MAX_ATTEMPTS", "5");
		over.getMirror().put("ENABLED", "false");
		over.getOauth2().put("INVALIDATE_REFRESH_TOKENS", "true");
		over.getOauth2Client().put("USERNAME", "email");
		// TODO Assertions
		over.getOpenid().put("ENABLE_AUTO_REGISTRATION","true");
		over.getOther().put("ENABLE_FEED","");
		over.getPackages().put("LIMIT_TOTAL_OWNER_SIZE","500 M");
		over.getPicture().put("GRAVATAR_SOURCE","duoshuo");
		over.getProject().put("PROJECT_BOARD_BASIC_KANBAN_TYPE","To Do, In Progress, Done, Cancel");
		over.getProxy().put("PROXY_HOSTS","*.example.com");
		over.getQueue().put("LENGTH","150");
		over.getRepoArchive().put("STORAGE_TYPE","local");
		over.getRepository().put("DEFAULT_PRIVATE","public");
		over.getRepositoryEditor().put("LINE_WRAP_EXTENSIONS",".txt,.md,.markdown,.mdown,.mkd");
		over.getRepositoryIssue().put("ENABLED","false");
		over.getRepositoryLocal().put("LOCAL_COPY_PATH","tmp/local-repo");
		//TODO Leads to https://github.com/Javatar81/devjoy/issues/22 over.getRepositoryMimeTypeMapping().put("","");
		over.getRepositoryPullRequest().put("WORK_IN_PROGRESS_PREFIXES","WIP:,[WIP]:,WIPT:");
		over.getRepositoryRelease().put("ALLOWED_TYPES",".zip");
		over.getRepositorySigning().put("INITIAL_COMMIT","never");
		over.getRepositoryUpload().put("ALLOWED_TYPES",".zip");
		over.getSecurity().put("LOGIN_REMEMBER_DAYS","5");
		over.getServer().put("ALLOW_GRACEFUL_RESTARTS","false");
		over.getService().put("ENABLE_BASIC_AUTHENTICATION","true");
		over.getServiceExplore().put("REQUIRE_SIGNIN_VIEW", "true");
		over.getSession().put("COOKIE_NAME", "i_like_gitea_devjoy");
		over.getSshMinimumKeySizes().put("DSA", "1024");
		over.getStorage().put("SERVE_DIRECT", "true");
		over.getStorageRepoArchive().put("SERVE_DIRECT", "true");
		over.getTask().put("QUEUE_LENGTH", "1024");
		//over.getTime().put("DEFAULT_UI_LOCATION", "Asia/Shanghai");
		over.getUi().put("EXPLORE_PAGING_NUM", "30");
		over.getUiAdmin().put("USER_PAGING_NUM", "60");
		over.getUiCsv().put("MAX_FILE_SIZE", "524289");
		over.getUiMeta().put("AUTHOR", "Gitea - Git with a cup of tea with devjoy");
		over.getUiNotification().put("MIN_TIMEOUT", "11s");
		over.getUiSvg().put("ENABLE_RENDER", "false");
		over.getUiUser().put("REPO_PAGING_NUM", "20");
		over.getWebhook().put("QUEUE_LENGTH", "1001");

		env.createStaticPVsIfRequired();
		
		
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(360, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			assertions.assertGiteaRoute(gitea);
			assertions.assertMailerConfig(gitea);
			assertions.assertOverrides(gitea);
		});
	}
}
