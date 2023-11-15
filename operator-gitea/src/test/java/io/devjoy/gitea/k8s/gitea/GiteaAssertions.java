package io.devjoy.gitea.k8s.gitea;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.util.Base64;
import java.util.Comparator;
import java.util.Optional;

import org.hamcrest.core.IsNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.GiteaAppIni;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaMailerSpec;
import io.devjoy.gitea.k8s.postgres.PostgresDeploymentDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresPvcDependentResource;
import io.devjoy.gitea.k8s.postgres.PostgresSecretDependentResource;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.ReplicaSet;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GiteaAssertions {
    private static final Logger LOG = LoggerFactory.getLogger(GiteaAssertions.class);
    @Inject
    OpenShiftClient client;

    public void assertPostgresPvc(Gitea desired) {
        final var postgresPvc = client.persistentVolumeClaims()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresPvcDependentResource.getName(desired)).get();
        assertThat(postgresPvc, is(IsNull.notNullValue()));
        if (!StringUtil.isNullOrEmpty(desired.getSpec().getPostgres().getStorageClass())) {
            assertThat(postgresPvc.getSpec().getStorageClassName(), is(desired.getSpec().getPostgres().getStorageClass()));
        }
        assertThat(postgresPvc.getSpec().getResources().getRequests().get("storage").toString(), is(desired.getSpec().getPostgres().getVolumeSize()));
        assertThat(postgresPvc.getStatus().getPhase(), is("Bound"));
    }

    public void assertGiteaPvc(Gitea desired) {
        final var giteaPvc = client.persistentVolumeClaims()
                    .inNamespace(desired.getMetadata().getNamespace())
                    .withName(GiteaPvcDependentResource.getName(desired)).get();
        assertThat(giteaPvc, is(IsNull.notNullValue()));
        if (!StringUtil.isNullOrEmpty(desired.getSpec().getStorageClass())) {
            assertThat(giteaPvc.getSpec().getStorageClassName(), is(desired.getSpec().getStorageClass()));
        }
        assertThat(giteaPvc.getSpec().getResources().getRequests().get("storage").toString(), is(desired.getSpec().getVolumeSize()));
        assertThat(giteaPvc.getStatus().getPhase(), is("Bound"));
    }

    public void assertAdminSecret(Gitea desired) {
        LOG.debug("Assert admin secret");
        final var adminSecret = GiteaAdminSecretDependentResource.getResource(desired, client);
        assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("user"))), is(desired.getSpec().getAdminUser()));
        assertThat(new String(java.util.Base64.getDecoder().decode(adminSecret.get().getData().get("password"))), is(IsNull.notNullValue()));
        LOG.debug("Asserted admin secret for Postgres");
    }

    public void assertGitea(Gitea desired) {
        final var gitea = client.resources(Gitea.class)
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(desired.getMetadata().getName())
                .get();
        assertThat(gitea.getSpec().getAdminPassword(), is(IsNull.notNullValue()));
    }

    public void assertGiteaDeployment(Gitea desired) {
        LOG.debug("Assert Gitea Deployment");
        final var postgresDeployment = client.apps().deployments()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresDeploymentDependentResource.getName(desired)).get();
        final var postgresPvc = client.persistentVolumeClaims()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(PostgresPvcDependentResource.getName(desired)).get();
        var postgresSecret = client.secrets()
                    .inNamespace(desired.getMetadata().getNamespace())
                    .withName(PostgresSecretDependentResource.getName(desired)).get();

        assertThat(postgresDeployment, is(IsNull.notNullValue()));
        assertThat(postgresSecret, is(IsNull.notNullValue()));
        assertThat(postgresPvc, is(IsNull.notNullValue()));
        if (!desired.getSpec().isResourceRequirementsEnabled()) {
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getVolumes().stream().filter(v -> "postgresql-data".equals(v.getName())).map(v -> v.getPersistentVolumeClaim().getClaimName()).findFirst().get(), is(postgresPvc.getMetadata().getName()));
            LOG.debug("Asserted resources is empty for Postgres");
        } else {
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("memory"), is(Quantity.parse(desired.getSpec().getPostgres().getMemoryRequest())));
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("cpu"), is(Quantity.parse(desired.getSpec().getPostgres().getCpuRequest())));
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("memory"), is(Quantity.parse(desired.getSpec().getPostgres().getMemoryLimit())));
            assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("cpu"), is(Quantity.parse(desired.getSpec().getPostgres().getCpuLimit())));
            LOG.debug("Asserted Resources for Postgres");
        }
        

        assertThat(postgresDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv().stream()
            .filter(e -> "POSTGRESQL_PASSWORD".equals(e.getName())).findAny().get().getValueFrom().getSecretKeyRef().getName(), is(postgresSecret.getMetadata().getName()));
        assertThat(postgresDeployment.getStatus().getReadyReplicas(), is(1));
        // RS
        Optional<ReplicaSet> postgresReplicaSet = client.apps().replicaSets()
            .inNamespace(desired.getMetadata().getNamespace())
            .list()
            .getItems()
            .stream()
            .filter(r -> r.getOwnerReferenceFor(postgresDeployment.getMetadata().getUid()).isPresent())
            .max(Comparator.comparingInt(r -> Integer.valueOf(r.getMetadata().getAnnotations().get("deployment.kubernetes.io/revision"))));
        assertThat(postgresReplicaSet.isPresent(), is(true));
        assertThat(postgresReplicaSet.get().getStatus().getReadyReplicas(), is(1));
        //Pod
        Optional<Pod> pod = client.pods()
            .inNamespace(desired.getMetadata().getNamespace())
            .list()
            .getItems()
            .stream()
            .filter(p -> p.getOwnerReferenceFor(postgresReplicaSet.get().getMetadata().getUid()).isPresent())
            .findAny();
        assertThat(pod.isPresent(), is(true));
        final var giteaDeployment = client.apps().deployments()
                .inNamespace(desired.getMetadata().getNamespace())
                .withName(desired.getMetadata().getName()).get();
        assertThat(giteaDeployment, is(IsNull.notNullValue()));
        if (!desired.getSpec().isResourceRequirementsEnabled()) {
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().isEmpty(), is(true));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().isEmpty(), is(true));
            assertThat(giteaDeployment.getStatus().getReadyReplicas(), is(1));
            LOG.debug("Asserted resources is empty for Gitea");
        } else {
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("memory"), is(Quantity.parse(desired.getSpec().getMemoryRequest())));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getRequests().get("cpu"), is(Quantity.parse(desired.getSpec().getCpuRequest())));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("memory"), is(Quantity.parse(desired.getSpec().getMemoryLimit())));
            assertThat(giteaDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getResources().getLimits().get("cpu"), is(Quantity.parse(desired.getSpec().getCpuLimit())));
            LOG.debug("Asserted Resources for Postgres");
        }
       
        LOG.debug("Asserted deployment for Gitea");
    }

    public void assertGiteaRoute(Gitea desired) {
         if (desired.getSpec().isIngressEnabled() && !StringUtil.isNullOrEmpty(desired.getSpec().getRoute())) {
            assertThat(client.routes().inNamespace(desired.getMetadata().getNamespace()).withName(GiteaRouteDependentResource.getName(desired)).get().getSpec().getHost(), is(desired.getSpec().getRoute()));
            LOG.debug("Asserted route for Gitea");
        }
    }

    public void assertMailerConfig(Gitea desired) throws IOException {
        Resource<Secret> config = GiteaConfigSecretDependentResource.getResource(desired, client);
        String iniData = new String(Base64.getDecoder().decode(config.get().getData().get(GiteaConfigSecretDependentResource.KEY_APP_INI)));
        GiteaAppIni iniConfiguration = new GiteaAppIni(iniData);
        GiteaMailerSpec mailer = desired.getSpec().getMailer();
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("FROM"), is(mailer.getFrom()));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("PROTOCOL"), is(mailer.getProtocol()));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("SMTP_ADDR"), is(mailer.getHost()));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("USER"), is(mailer.getUser()));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("PASSWD"), is(mailer.getPassword()));
    }

    public void assertOverrides(Gitea desired) throws IOException {
        Resource<Secret> config = GiteaConfigSecretDependentResource.getResource(desired, client);
        String iniData = new String(Base64.getDecoder().decode(config.get().getData().get(GiteaConfigSecretDependentResource.KEY_APP_INI)));
        GiteaAppIni iniConfiguration = new GiteaAppIni(iniData);
	
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_ACTIONS).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getActions().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_ADMIN).getProperty("DEFAULT_EMAIL_NOTIFICATIONS"), is(desired.getSpec().getConfigOverrides().getAdmin().get("DEFAULT_EMAIL_NOTIFICATIONS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_API).getProperty("MAX_RESPONSE_ITEMS"), is(desired.getSpec().getConfigOverrides().getApi().get("MAX_RESPONSE_ITEMS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_ATTACHMENT).getProperty("MAX_SIZE"), is(desired.getSpec().getConfigOverrides().getAttachment().get("MAX_SIZE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CACHE).getProperty("INTERVAL"), is(desired.getSpec().getConfigOverrides().getCache().get("INTERVAL")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CACHE_LAST_COMMIT).getProperty("ITEM_TTL"), is(desired.getSpec().getConfigOverrides().getCacheLastCommit().get("ITEM_TTL")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CAMO).getProperty("SERVER_URL"), is(desired.getSpec().getConfigOverrides().getCamo().get("SERVER_URL")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CORS).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getCors().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getCron().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_ARCHIVE_CLEANUP).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getCronArchiveCleanup().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_CHECK_REPO_STATS).getProperty("RUN_AT_START"), is(desired.getSpec().getConfigOverrides().getCronCheckRepoStats().get("RUN_AT_START")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_CLEANUP_HOOK_TASK_TABLE).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getCronCleanupHookTaskTable().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_CLEANUP_PACKAGES).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getCronCleanupPackages().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_REPO_HEALTH_CHECK).getProperty("TIMEOUT"), is(desired.getSpec().getConfigOverrides().getCronRepoHealthCheck().get("TIMEOUT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_SYNC_EXTERNAL_USERS).getProperty("UPDATE_EXISTING"), is(desired.getSpec().getConfigOverrides().getCronSyncExternalUsers().get("UPDATE_EXISTING")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_UPDATE_MIGRATION_POSTER_ID).getProperty("SCHEDULE"), is(desired.getSpec().getConfigOverrides().getCronUpdateMigrationPosterId().get("SCHEDULE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_CRON_UPDATE_MIRRORS).getProperty("PULL_LIMIT"), is(desired.getSpec().getConfigOverrides().getCronUpdateMirrors().get("PULL_LIMIT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_DATABASE).getProperty("DB_RETRIES"), is(desired.getSpec().getConfigOverrides().getDatabase().get("DB_RETRIES")));
        assertThat(iniConfiguration.getProperty("APP_NAME"), is(desired.getSpec().getConfigOverrides().getDefaults().get("APP_NAME")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_EMAIL_INCOMING).getProperty("USERNAME"), is(desired.getSpec().getConfigOverrides().getEmailIncoming().get("USERNAME")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_FEDERATION).getProperty("MAX_SIZE"), is(desired.getSpec().getConfigOverrides().getFederation().get("MAX_SIZE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_GIT).getProperty("DISABLE_PARTIAL_CLONE"), is(desired.getSpec().getConfigOverrides().getGit().get("DISABLE_PARTIAL_CLONE")));
        //assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_GIT_CONFIG).getProperty("core.logAllRefUpdates"), is(desired.getSpec().getConfigOverrides().getGitConfig().get("core.logAllRefUpdates")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_GIT_TIMEOUT).getProperty("DEFAULT"), is(desired.getSpec().getConfigOverrides().getGitTimeout().get("DEFAULT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_HIGHLIGHT_MAPPING).getProperty("file_extension"), is(desired.getSpec().getConfigOverrides().getHighlightMapping().get("file_extension")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_I18N).getProperty("LANGS"), is(desired.getSpec().getConfigOverrides().getI18n().get("LANGS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_INDEXER).getProperty("REPO_INDEXER_TYPE"), is(desired.getSpec().getConfigOverrides().getIndexer().get("REPO_INDEXER_TYPE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_LFS).getProperty("MINIO_INSECURE_SKIP_VERIFY"), is(desired.getSpec().getConfigOverrides().getLfs().get("MINIO_INSECURE_SKIP_VERIFY")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_LOG).getProperty("ENABLE_SSH_LOG"), is(desired.getSpec().getConfigOverrides().getLog().get("ENABLE_SSH_LOG")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_LOG_CONN).getProperty("RECONNECT"), is(desired.getSpec().getConfigOverrides().getLogConn().get("RECONNECT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_LOG_CONSOLE).getProperty("STDERR"), is(desired.getSpec().getConfigOverrides().getLogConsole().get("STDERR")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_LOG_FILE).getProperty("FILE_NAME"), is(desired.getSpec().getConfigOverrides().getLogFile().get("FILE_NAME")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MAILER).getProperty("SUBJECT_PREFIX"), is(desired.getSpec().getConfigOverrides().getMailer().get("SUBJECT_PREFIX")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MARKDOWN).getProperty("ENABLE_MATH"), is(desired.getSpec().getConfigOverrides().getMarkdown().get("ENABLE_MATH")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_METRICS).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getMetrics().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MIGRATIONS).getProperty("MAX_ATTEMPTS"), is(desired.getSpec().getConfigOverrides().getMigrations().get("MAX_ATTEMPTS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_MIRROR).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getMirror().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_OAUTH2).getProperty("INVALIDATE_REFRESH_TOKENS"), is(desired.getSpec().getConfigOverrides().getOauth2().get("INVALIDATE_REFRESH_TOKENS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_OAUTH2_CLIENT).getProperty("USERNAME"), is(desired.getSpec().getConfigOverrides().getOauth2Client().get("USERNAME")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_OPENID).getProperty("ENABLE_AUTO_REGISTRATION"), is(desired.getSpec().getConfigOverrides().getOpenid().get("ENABLE_AUTO_REGISTRATION")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_OTHER).getProperty("ENABLE_FEED"), is(desired.getSpec().getConfigOverrides().getOther().get("ENABLE_FEED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_PACKAGES).getProperty("LIMIT_TOTAL_OWNER_SIZE"), is(desired.getSpec().getConfigOverrides().getPackages().get("LIMIT_TOTAL_OWNER_SIZE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_PICTURE).getProperty("GRAVATAR_SOURCE"), is(desired.getSpec().getConfigOverrides().getPicture().get("GRAVATAR_SOURCE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_PROJECT).getProperty("PROJECT_BOARD_BASIC_KANBAN_TYPE"), is(desired.getSpec().getConfigOverrides().getProject().get("PROJECT_BOARD_BASIC_KANBAN_TYPE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_PROXY).getProperty("PROXY_HOSTS"), is(desired.getSpec().getConfigOverrides().getProxy().get("PROXY_HOSTS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_QUEUE).getProperty("LENGTH"), is(desired.getSpec().getConfigOverrides().getQueue().get("LENGTH")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPO_ARCHIVE).getProperty("STORAGE_TYPE"), is(desired.getSpec().getConfigOverrides().getRepoArchive().get("STORAGE_TYPE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY).getProperty("DEFAULT_PRIVATE"), is(desired.getSpec().getConfigOverrides().getRepository().get("DEFAULT_PRIVATE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_EDITOR).getProperty("LINE_WRAP_EXTENSIONS"), is(desired.getSpec().getConfigOverrides().getRepositoryEditor().get("LINE_WRAP_EXTENSIONS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_ISSUE).getProperty("ENABLED"), is(desired.getSpec().getConfigOverrides().getRepositoryIssue().get("ENABLED")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_LOCAL).getProperty("LOCAL_COPY_PATH"), is(desired.getSpec().getConfigOverrides().getRepositoryLocal().get("LOCAL_COPY_PATH")));
        //TODO Leads to https://github.com/Javatar81/devjoy/issues/22 over.getRepositoryMimeTypeMapping().put("","");
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_PULL_REQUEST).getProperty("WORK_IN_PROGRESS_PREFIXES"), is(desired.getSpec().getConfigOverrides().getRepositoryPullRequest().get("WORK_IN_PROGRESS_PREFIXES")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_RELEASE).getProperty("ALLOWED_TYPES"), is(desired.getSpec().getConfigOverrides().getRepositoryRelease().get("ALLOWED_TYPES")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_SIGNING).getProperty("INITIAL_COMMIT"), is(desired.getSpec().getConfigOverrides().getRepositorySigning().get("INITIAL_COMMIT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_REPOSITORY_UPLOAD).getProperty("ALLOWED_TYPES"), is(desired.getSpec().getConfigOverrides().getRepositoryUpload().get("ALLOWED_TYPES")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SECURITY).getProperty("LOGIN_REMEMBER_DAYS"), is(desired.getSpec().getConfigOverrides().getSecurity().get("LOGIN_REMEMBER_DAYS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SERVER).getProperty("ALLOW_GRACEFUL_RESTARTS"), is(desired.getSpec().getConfigOverrides().getServer().get("ALLOW_GRACEFUL_RESTARTS")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SERVICE).getProperty("ENABLE_BASIC_AUTHENTICATION"), is(desired.getSpec().getConfigOverrides().getService().get("ENABLE_BASIC_AUTHENTICATION")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SERVICE_EXPLORE).getProperty("REQUIRE_SIGNIN_VIEW"), is(desired.getSpec().getConfigOverrides().getServiceExplore().get("REQUIRE_SIGNIN_VIEW")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SESSION).getProperty("COOKIE_NAME"), is(desired.getSpec().getConfigOverrides().getSession().get("COOKIE_NAME")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_SSH_MINIMUM_KEY_SIZES).getProperty("DSA"), is(desired.getSpec().getConfigOverrides().getSshMinimumKeySizes().get("DSA")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_STORAGE).getProperty("SERVE_DIRECT"), is(desired.getSpec().getConfigOverrides().getStorage().get("SERVE_DIRECT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_STORAGE_REPO_ARCHIVE).getProperty("ENABLE_BASIC_AUTHENTICATION"), is(desired.getSpec().getConfigOverrides().getStorageRepoArchive().get("ENABLE_BASIC_AUTHENTICATION")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_TASK).getProperty("QUEUE_LENGTH"), is(desired.getSpec().getConfigOverrides().getTask().get("QUEUE_LENGTH")));
        //TODO assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_TIME).getProperty("DEFAULT_UI_LOCATION"), is(desired.getSpec().getConfigOverrides().getTime().get("DEFAULT_UI_LOCATION")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI).getProperty("EXPLORE_PAGING_NUM"), is(desired.getSpec().getConfigOverrides().getUi().get("EXPLORE_PAGING_NUM")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_ADMIN).getProperty("USER_PAGING_NUM"), is(desired.getSpec().getConfigOverrides().getUiAdmin().get("USER_PAGING_NUM")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_CSV).getProperty("MAX_FILE_SIZE"), is(desired.getSpec().getConfigOverrides().getUiCsv().get("MAX_FILE_SIZE")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_META).getProperty("AUTHOR"), is(desired.getSpec().getConfigOverrides().getUiMeta().get("AUTHOR")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_NOTIFICATION).getProperty("MIN_TIMEOUT"), is(desired.getSpec().getConfigOverrides().getUiNotification().get("MIN_TIMEOUT")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_SVG).getProperty("ENABLE_RENDER"), is(desired.getSpec().getConfigOverrides().getUiSvg().get("ENABLE_RENDER")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_UI_USER).getProperty("REPO_PAGING_NUM"), is(desired.getSpec().getConfigOverrides().getUiUser().get("REPO_PAGING_NUM")));
        assertThat(iniConfiguration.getSection(GiteaConfigSecretDependentResource.SECTION_WEBHOOK).getProperty("QUEUE_LENGTH"), is(desired.getSpec().getConfigOverrides().getWebhook().get("QUEUE_LENGTH")));
    }
}
