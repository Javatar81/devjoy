package io.devjoy.gitea.k8s;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.hamcrest.core.IsNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.devjoy.gitea.domain.service.GiteaApiService;
import io.devjoy.gitea.domain.service.UserService;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaAssertions;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakClientDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakDependentResource;
import io.devjoy.gitea.k8s.dependent.rhsso.KeycloakRealmDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConfigOverrides;
import io.devjoy.gitea.k8s.model.GiteaLogLevel;
import io.devjoy.gitea.k8s.model.GiteaSpec;
import io.devjoy.gitea.util.ApiAccessMode;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.QuantityBuilder;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class GiteaReconcilerIT {

	OpenShiftClient client = new KubernetesClientBuilder().build().adapt(OpenShiftClient.class);
	ApiAccessMode accessMode = ConfigProviderResolver.instance().getConfig().getValue("io.devjoy.gitea.api.access.mode", ApiAccessMode.class);
	TestEnvironment env = new TestEnvironment(client);
	GiteaApiService apiService = new GiteaApiService(client);
	{
		apiService.setAccessMode(accessMode);
		
	}
	
	UserService userService = new UserService(apiService);
	
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

	Gitea createDefault(String name) {
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
		Quantity volumeSize = new QuantityBuilder().withAmount("1").withFormat("Gi").build();
		spec.getPostgres().setVolumeSize(volumeSize.getAmount() + volumeSize.getFormat());
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
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(10));
        });
	}

	@Test
	void createMinimalGitea() {
		Gitea gitea = createDefault("mygiteait");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(120, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))).length(), is(gitea.getSpec().getAdminPasswordLength()));
        });
	}

	@Test
	void createGiteaWithSso() {
		Gitea gitea = createDefault("mygiteait");
		gitea.getSpec().setSso(true);
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(400, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			assertThat(KeycloakDependentResource.getResource(gitea, client).get().getStatus().getReady(), is(true));
			assertThat(KeycloakRealmDependentResource.getResource(gitea, client).get().getStatus().getReady(), is(true));
			assertThat(KeycloakClientDependentResource.getResource(gitea, client).get().getStatus().getReady(), is(true));
		});
	}

	@Test
	void createGiteaWithPassword() {
		Gitea gitea = createDefault("mygiteaitwpw");
		String password = "test1234";
		gitea.getSpec().setAdminPassword(password);
		gitea.getSpec().setAdminPasswordLength(password.length());
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		await().ignoreException(NullPointerException.class).atMost(180, TimeUnit.SECONDS).untilAsserted(() -> {
            // check that we create the deployment
            // Postgres PVC
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client);
			assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(gitea.getSpec().getAdminPassword()));
        });
	}

	@Test
	void changeGiteaPassword() {
		String changedPassword = "pwchange123"; // notsecret
		Gitea gitea = createDefault("pwchange");
		env.createStaticPVsIfRequired();
		client.resource(gitea).create();
		client.apps().deployments()
                .inNamespace(getTargetNamespace())
                .withName(gitea.getMetadata().getName()).waitUntilCondition(c -> c != null && c.getStatus().getReadyReplicas() != null && c.getStatus().getReadyReplicas() == 1, 120, TimeUnit.SECONDS);
		await().atMost(30, TimeUnit.SECONDS).then().until(() -> client.resources(Gitea.class).inNamespace(getTargetNamespace()).withName("pwchange").edit(n -> {
			n.getSpec().setAdminPassword(changedPassword);
			return n;
			
		}) != null)
		;
		await().ignoreException(NullPointerException.class).atMost(150, TimeUnit.SECONDS).untilAsserted(() -> {
			assertions.assertPostgresPvc(gitea);
			assertions.assertGiteaPvc(gitea);
			assertions.assertGiteaDeployment(gitea);
			assertions.assertAdminSecret(gitea);
			final var adminSecret = GiteaAdminSecretDependentResource.getResource(gitea, client).get();
			assertThat(adminSecret, is(IsNull.notNullValue()));
			assertThat(GiteaAdminSecretDependentResource.getAdminPassword(adminSecret).get(), is(changedPassword));
			//System.out.println(GiteaAdminSecretDependentResource.getAdminPassword(adminSecret).get());
			//Check if Admin can login by trying to create a token with the new password
			assertThat(userService.createAccessToken(gitea, gitea.getSpec().getAdminUser(), changedPassword, "testpwchg", UserService.SCOPE_WRITE_REPO).isEmpty(), is(false));
			//AccessToken token = tokenService.createUserToken("http://" + host, gitea.getSpec().getAdminUser(), changedPassword);
			//assertThat(token,  is(IsNull.notNullValue()));
        });
	}

	@Test
	void createGiteaWitAllFields() {
		Gitea gitea = createDefault("allfields");
		GiteaSpec spec = gitea.getSpec();
		spec.setSso(true);
		spec.setAdminPassword("test12345"); // notsecret
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
		String baseDomain = client.config().ingresses().withName("cluster").get().getSpec().getDomain();
		spec.setRoute(String.format("%s-%s.%s", "gitea", getTargetNamespace(), baseDomain));
		spec.getMailer().setEnabled(true);
		spec.getMailer().setFrom("gitea-devjoy@example.com");
		spec.getMailer().setHeloHostname("gitea");
		spec.getMailer().setHost("example.com");
		spec.getMailer().setPassword("test12345"); // notsecretnull);
		spec.getMailer().setProtocol("smtp");
		spec.getMailer().setUser("giteadm");
		spec.getPostgres().setMemoryLimit("1Gi");
		spec.getPostgres().setMemoryRequest("256Mi");
		spec.getPostgres().setCpuLimit("800m");
		spec.getPostgres().setCpuRequest("250m");
		spec.getPostgres().setImage("registry.redhat.io/rhel8/postgresql-12");
		spec.getPostgres().setImageTag("latest");
		spec.getPostgres().setVolumeSize("8Gi");
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
