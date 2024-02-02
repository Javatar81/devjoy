package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.GiteaAppIni;
import io.devjoy.gitea.k8s.dependent.postgres.PostgresConfig;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaConfigOverrides;
import io.devjoy.gitea.k8s.model.GiteaMailerSpec;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;
import jakarta.inject.Inject;

@KubernetesDependent(resourceDiscriminator = GiteaConfigSecretDiscriminator.class, labelSelector = GiteaConfigSecretDependentResource.LABEL_SELECTOR)
public class GiteaConfigSecretDependentResource extends CRUDKubernetesDependentResource<Secret, Gitea>{
	static final Logger LOG = LoggerFactory.getLogger(GiteaConfigSecretDependentResource.class);
	static final String SECTION_DATABASE = "database";
	static final String SECTION_MIGRATIONS = "migrations";
	static final String SECTION_SERVER = "server";
	static final String SECTION_MAILER = "mailer";
	static final String SECTION_SERVICE = "service";
	static final String SECTION_REPOSITORY = "repository";
	static final String SECTION_REPOSITORY_EDITOR = "repository.editor";
	static final String SECTION_REPOSITORY_PULL_REQUEST = "repository.pull-request";
	static final String SECTION_REPOSITORY_ISSUE = "repository.issue";
	static final String SECTION_REPOSITORY_UPLOAD= "repository.upload";
	static final String SECTION_REPOSITORY_RELEASE = "repository.release";
	static final String SECTION_REPOSITORY_SIGNING = "repository.signing";
	static final String SECTION_REPOSITORY_LOCAL = "repository.local";
	static final String SECTION_REPOSITORY_MIME_TYPE_MAPPING = "repository.mimetype_mapping";
	static final String SECTION_CORS = "cors";
	static final String SECTION_UI = "ui";
	static final String SECTION_UI_ADMIN = "ui.admin";
	static final String SECTION_UI_USER = "ui.user";
	static final String SECTION_UI_META = "ui.meta";
	static final String SECTION_UI_NOTIFICATION = "ui.notification";
	static final String SECTION_UI_SVG = "ui.svg";
	static final String SECTION_UI_CSV = "ui.csv";
	static final String SECTION_MARKDOWN = "markdown";
	static final String SECTION_INDEXER = "indexer";
	static final String SECTION_QUEUE = "queue";
	static final String SECTION_ADMIN = "admin";
	static final String SECTION_SECURITY = "security";
	static final String SECTION_CAMO = "camo";
	static final String SECTION_OPENID = "openid";
	static final String SECTION_OAUTH2_CLIENT = "oauth2_client";
	static final String SECTION_SERVICE_EXPLORE = "service.explore";
	static final String SECTION_SSH_MINIMUM_KEY_SIZES= "ssh.minimum_key_sizes";
	static final String SECTION_WEBHOOK = "webhook";
	static final String SECTION_EMAIL_INCOMING = "email.incoming";
	static final String SECTION_CACHE = "cache";
	static final String SECTION_CACHE_LAST_COMMIT = "cache.last_commit";
	static final String SECTION_SESSION = "session";
	static final String SECTION_PICTURE= "picture";
	static final String SECTION_PROJECT = "project";
	static final String SECTION_ATTACHMENT = "attachment";
	static final String SECTION_LOG_CONSOLE = "log.console";
	static final String SECTION_LOG_FILE = "log.file";
	static final String SECTION_LOG_CONN = "log.conn";
    static final String SECTION_CRON = "cron";
    static final String SECTION_CRON_ARCHIVE_CLEANUP = "cron.archive_cleanup";
    static final String SECTION_CRON_UPDATE_MIRRORS = "cron.update_mirrors";
    static final String SECTION_CRON_REPO_HEALTH_CHECK = "cron.repo_health_check";
    static final String SECTION_CRON_CHECK_REPO_STATS = "cron.check_repo_stats";
    static final String SECTION_CRON_CLEANUP_HOOK_TASK_TABLE = "cron.cleanup_hook_task_table";
    static final String SECTION_CRON_CLEANUP_PACKAGES = "cron.cron.cleanup_packages";
    static final String SECTION_CRON_UPDATE_MIGRATION_POSTER_ID = "cron.update_migration_poster_id";
    static final String SECTION_CRON_SYNC_EXTERNAL_USERS = "cron.sync_external_users";
    static final String SECTION_GIT = "git";
    static final String SECTION_GIT_TIMEOUT = "git.timeout";
    static final String SECTION_GIT_CONFIG = "git.config";
    static final String SECTION_METRICS = "metrics";
    static final String SECTION_API = "api";
    static final String SECTION_OAUTH2 = "oauth2";
    static final String SECTION_I18N= "i18n";
    static final String SECTION_MARKUP = "markup";
    static final String SECTION_HIGHLIGHT_MAPPING = "highlight.mapping";
    static final String SECTION_TIME = "time";
    static final String SECTION_TASK = "task";
    static final String SECTION_FEDERATION = "federation";
    static final String SECTION_PACKAGES = "packages";
    static final String SECTION_MIRROR = "mirror";
    static final String SECTION_LFS = "lfs";
    static final String SECTION_STORAGE = "storage";
    static final String SECTION_STORAGE_REPO_ARCHIVE = "storage.repo-archive";
    static final String SECTION_REPO_ARCHIVE = "repo-archive";
    static final String SECTION_PROXY = "proxy";
    static final String SECTION_ACTIONS = "actions";
    static final String SECTION_OTHER = "other";
	static final String SECTION_LOG = "log";
	static final String LABEL_KEY = "devjoy.io/configsecret.target";
	static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	static final String KEY_APP_INI = "app.ini";

	@Inject
	PostgresConfig config;
	@Inject
	OpenShiftClient ocpClient;
	
	public GiteaConfigSecretDependentResource() {
		super(Secret.class);
	}

	@Override
	public Secret update(Secret actual, Secret target, Gitea primary, Context<Gitea> context) {
		var updated = super.update(actual, target, primary, context);
		LOG.info("Restarting deployment due to config change");
		context.getClient().apps().deployments().inNamespace(actual.getMetadata().getNamespace())
				.withName(actual.getMetadata().getOwnerReferences().get(0).getName()).rolling().restart();
		return updated;
	  }
	
	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea config map");
		Secret cm = context.getClient()
				.secrets()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/config-secret.yaml"))
				.item();
		
		String name = getName(primary);
		cm.getMetadata().setName(name);
		cm.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (cm.getMetadata().getLabels() == null) {
			cm.getMetadata().setLabels(new HashMap<>());
		}
		cm.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		if (cm.getMetadata().getAnnotations() == null) {
			cm.getMetadata().setAnnotations(new HashMap<>());
		}
		String iniData = cm.getStringData().get(KEY_APP_INI);
		GiteaAppIni iniConfiguration = new GiteaAppIni(iniData);
		LOG.debug("Reading app.ini initial data {}", iniData);
		LOG.info("Read app.ini. Adding configuration parameters based on Gitea resource.");
		iniConfiguration.setProperty("APP_NAME", primary.getMetadata().getName());
		configureDatabase(primary, iniConfiguration);
		if((primary.getSpec() == null || primary.getSpec().isIngressEnabled()) && ocpClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)){
			Optional<Route> route = Optional.ofNullable(GiteaRouteDependentResource.getResource(primary, ocpClient)
				.waitUntilCondition(c -> c!= null && !StringUtil.isNullOrEmpty(c.getSpec().getHost()), 30, TimeUnit.SECONDS));
			route.ifPresent(r -> configureRoute(iniConfiguration, r, primary.getSpec() != null && primary.getSpec().isSsl()));
		}
		configureServer(iniConfiguration);
		iniConfiguration.getSection(SECTION_MIGRATIONS).setProperty("ALLOW_LOCALNETWORKS", "false");
		
		if (primary.getSpec() != null && primary.getSpec().getLogLevel() != null) {
			iniConfiguration.getSection(SECTION_LOG).setProperty("LEVEL", primary.getSpec().getLogLevel());
		}
		if (primary.getSpec() != null && primary.getSpec().getMailer() != null) {
			configureMailer(iniConfiguration, primary.getSpec().getMailer());
		}
		configureService(primary, iniConfiguration);
		if (primary.getSpec() != null){
			addOverrides(primary, iniConfiguration);
		}
		String finalAppIni = iniConfiguration.toString();
		LOG.debug("app.ini after adding configuration parameters {}", finalAppIni);
		//cm.getStringData().put("app.ini", finalAppIni);
		cm.getStringData().clear();
		cm.setData(new HashMap<>());
		cm.getData().put("app.ini", new String(Base64.getEncoder().encode(
			finalAppIni.getBytes())));
		
		return cm;
	}

	private void addOverrides(Gitea primary, GiteaAppIni iniConfiguration) {
        GiteaConfigOverrides configOverrides = primary.getSpec().getConfigOverrides();
		configOverrides.getDefaults().entrySet().forEach( e -> iniConfiguration.setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepository().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepositoryEditor().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_EDITOR).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepositoryPullRequest().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_PULL_REQUEST).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getLog().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_LOG).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepositoryIssue().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_ISSUE).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepositoryUpload().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_UPLOAD).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getRepositoryRelease().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_RELEASE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getRepositorySigning().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_SIGNING).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getRepositoryLocal().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_LOCAL).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getRepositoryMimeTypeMapping().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPOSITORY_MIME_TYPE_MAPPING).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCors().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CORS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUi().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiAdmin().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_ADMIN).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiUser().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_USER).setProperty(e.getKey().toUpperCase(), e.getValue()));
		configOverrides.getUiMeta().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_META).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiNotification().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_NOTIFICATION).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiSvg().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_SVG).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiCsv().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_CSV).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMarkdown().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MARKDOWN).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getServer().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SERVER).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getDatabase().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_DATABASE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getIndexer().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_INDEXER).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getQueue().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_QUEUE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getAdmin().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_ADMIN).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getSecurity().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SECURITY).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCamo().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CAMO).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getOpenid().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_OPENID).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getOauth2Client().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_OAUTH2_CLIENT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getService().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SERVICE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getServiceExplore().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SERVICE_EXPLORE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getSshMinimumKeySizes().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SSH_MINIMUM_KEY_SIZES).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getWebhook().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_WEBHOOK).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMailer().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MAILER).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getEmailIncoming().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_EMAIL_INCOMING).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCache().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CACHE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCacheLastCommit().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CACHE_LAST_COMMIT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getSession().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SESSION).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getPicture().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_PICTURE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getProject().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_PROJECT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getAttachment().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_ATTACHMENT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getLogConsole().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_LOG_CONSOLE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getLogFile().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_LOG_FILE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getLogConn().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_LOG_CONN).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCron().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronArchiveCleanup().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_ARCHIVE_CLEANUP).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronUpdateMirrors().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_UPDATE_MIRRORS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronRepoHealthCheck().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_REPO_HEALTH_CHECK).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronCheckRepoStats().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_CHECK_REPO_STATS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronCleanupHookTaskTable().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_CLEANUP_HOOK_TASK_TABLE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronCleanupPackages().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_CLEANUP_PACKAGES).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronUpdateMigrationPosterId().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_UPDATE_MIGRATION_POSTER_ID).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronSyncExternalUsers().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_SYNC_EXTERNAL_USERS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getGit().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getGitConfig().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT_CONFIG).setProperty(e.getKey(), e.getValue()));
        configOverrides.getGitTimeout().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT_TIMEOUT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMetrics().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_METRICS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getApi().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_API).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getOauth2().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_OAUTH2).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getI18n().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_I18N).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMarkup().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MARKUP).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getHighlightMapping().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_HIGHLIGHT_MAPPING).setProperty(e.getKey(), e.getValue()));
        configOverrides.getTime().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_TIME).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getTask().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_TASK).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMigrations().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MIGRATIONS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getFederation().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_FEDERATION).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getPackages().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_PACKAGES).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMirror().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MIRROR).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getLfs().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_LFS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getStorage().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_STORAGE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getStorageRepoArchive().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_STORAGE_REPO_ARCHIVE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getRepoArchive().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_REPO_ARCHIVE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getProxy().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_PROXY).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getActions().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_ACTIONS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getOther().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_OTHER).setProperty(e.getKey().toUpperCase(), e.getValue()));
	}


	private void configureService(Gitea primary, GiteaAppIni iniConfiguration) {
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("REGISTER_EMAIL_CONFIRM", primary.getSpec() != null && primary.getSpec().isRegisterEmailConfirm());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_NOTIFY_MAIL", primary.getSpec() != null && primary.getSpec().getMailer().isEnableNotifyMail());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DISABLE_REGISTRATION", primary.getSpec() != null && primary.getSpec().isDisableRegistration());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_CAPTCHA", primary.getSpec() != null && primary.getSpec().isEnableCaptcha());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DEFAULT_ALLOW_CREATE_ORGANIZATION", primary.getSpec() != null && primary.getSpec().isAllowCreateOrganization());
	}
	private void configureMailer(GiteaAppIni iniConfiguration, GiteaMailerSpec mailer) {
		iniConfiguration.getSection(SECTION_MAILER).setProperty("ENABLED", mailer.isEnabled());
		if (mailer.isEnabled()) {
			iniConfiguration.getSection(SECTION_MAILER).setProperty("FROM", mailer.getFrom());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("PROTOCOL", mailer.getProtocol());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("SMTP_ADDR", mailer.getHost());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("USER", mailer.getUser());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("PASSWD", mailer.getPassword());
			if (StringUtil.isNullOrEmpty(mailer.getHeloHostname())) {
				iniConfiguration.getSection(SECTION_MAILER).setProperty("HELO_HOSTNAME", mailer.getHeloHostname());
			}
		}
	}
	private void configureServer(GiteaAppIni iniConfiguration) {
		iniConfiguration.getSection(SECTION_SERVER).setProperty("HTTP_PORT", "3000");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_PORT", "2022");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_PORT", "2022");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("DISABLE_SSH", "true");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("START_SSH_SERVER", "false");
	}
	private void configureRoute(GiteaAppIni iniConfiguration, Route r, boolean ssl) {
		String protocol = "http" + (ssl ? "s://" : "://");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("ROOT_URL", protocol + r.getSpec().getHost() + "/");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_DOMAIN", r.getSpec().getHost());
		iniConfiguration.getSection(SECTION_SERVER).setProperty("DOMAIN", r.getSpec().getHost());
	}
	private void configureDatabase(Gitea primary, GiteaAppIni iniConfiguration) {
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("HOST", String.format("postgresql-%s.%s.svc:5432",primary.getMetadata().getName(), primary.getMetadata().getNamespace()));
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("NAME", config.getDatabaseName());
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("USER", config.getUserName());
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("PASSWD", config.getPassword());
		if (primary.getSpec() != null && primary.getSpec().getPostgres().isSsl()) {
			iniConfiguration.getSection(SECTION_DATABASE).setProperty("SSL_MODE", "verify-full");
		}
	}

	public static Resource<Secret> getResource(Gitea primary, KubernetesClient client) {
		return client.resources(Secret.class).inNamespace(primary.getMetadata().getNamespace()).withName(
				getName(primary));
	}

	public static String getName(Gitea primary) {
		return primary.getMetadata().getName() + "-config";
	}
}
