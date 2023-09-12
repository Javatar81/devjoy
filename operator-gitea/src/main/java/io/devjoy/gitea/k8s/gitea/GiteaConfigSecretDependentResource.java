package io.devjoy.gitea.k8s.gitea;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaConfigOverrides;
import io.devjoy.gitea.k8s.GiteaMailerSpec;
import io.devjoy.gitea.k8s.postgres.PostgresConfig;
import io.fabric8.kubernetes.api.model.Secret;
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
	private static final Logger LOG = LoggerFactory.getLogger(GiteaConfigSecretDependentResource.class);
	private static final String SECTION_DATABASE = "database";
	private static final String SECTION_MIGRATIONS = "migrations";
	private static final String SECTION_SERVER = "server";
	private static final String SECTION_MAILER = "mailer";
	private static final String SECTION_SERVICE = "service";
	private static final String SECTION_REPOSITORY = "repository";
	private static final String SECTION_REPOSITORY_EDITOR = "repository.editor";
	private static final String SECTION_REPOSITORY_PULL_REQUEST = "repository.pull-request";
	private static final String SECTION_REPOSITORY_ISSUE = "repository.issue";
	private static final String SECTION_REPOSITORY_UPLOAD= "repository.upload";
	private static final String SECTION_REPOSITORY_RELEASE = "repository.release";
	private static final String SECTION_REPOSITORY_SIGNING = "repository.signing";
	private static final String SECTION_REPOSITORY_LOCAL = "repository.local";
	private static final String SECTION_REPOSITORY_MIME_TYPE_MAPPING = "repository.mimetype_mapping";
	private static final String SECTION_CORS = "cors";
	private static final String SECTION_UI = "ui";
	private static final String SECTION_UI_ADMIN = "ui.admin";
	private static final String SECTION_UI_META = "ui.meta";
	private static final String SECTION_UI_NOTIFICATION = "ui.notification";
	private static final String SECTION_UI_SVG = "ui.svg";
	private static final String SECTION_UI_CSV = "ui.csv";
	private static final String SECTION_MARKDOWN = "markdown";
	private static final String SECTION_INDEX = "index";
	private static final String SECTION_QUEUE = "queue";
	private static final String SECTION_ADMIN = "admin";
	private static final String SECTION_SECURITY = "security";
	private static final String SECTION_CAMO = "camo";
	private static final String SECTION_OPENID = "openid";
	private static final String SECTION_OAUTH2_CLIENT = "oauth2_client";
	private static final String SECTION_SERVICE_EXPLORE = "service.explore";
	private static final String SECTION_SSH_MINIMUM_KEY_SIZES= "ssh.minimum_key_sizes";
	private static final String SECTION_WEBHOOK = "webhook";
	private static final String SECTION_EMAIL_INCOMING = "email.incoming";
	private static final String SECTION_CACHE = "cache";
	private static final String SECTION_CACHE_LAST_COMMIT = "cache.last_commit";
	private static final String SECTION_SESSION = "session";
	private static final String SECTION_PICTURE= "picture";
	private static final String SECTION_PROJECT = "project";
	private static final String SECTION_ATTACHMENT = "attachment";
	private static final String SECTION_LOG_CONSOLE = "log.console";
	private static final String SECTION_LOG_FILE = "log.file";
	private static final String SECTION_LOG_CONN = "log.conn";
    private static final String SECTION_CRON = "cron";
    private static final String SECTION_CRON_ARCHIVE_CLEANUP = "cron.archive_cleanup";
    private static final String SECTION_CRON_UPDATE_MIRRORS = "cron.update_mirrors";
    private static final String SECTION_CRON_REPO_HEALTH_CHECK = "cron.repo_health_check";
    private static final String SECTION_CRON_CHECK_REPO_STATS = "cron.check_repo_stats";
    private static final String SECTION_CRON_CLEANUP_HOOK_TASK_TABLE = "cron.cleanup_hook_task_table";
    private static final String SECTION_CRON_CLEANUP_PACKAGES = "cron.cron.cleanup_packages";
    private static final String SECTION_CRON_MIGRATION_POSTER_ID = "cron.update_migration_poster_id";
    private static final String SECTION_CRON_SYNC_EXTERNAL_USERS = "cron.sync_external_users";
    private static final String SECTION_GIT = "git";
    private static final String SECTION_GIT_TIMEOUT = "git.timeout";
    private static final String SECTION_GIT_CONFIG = "git.config";
    private static final String SECTION_METRICS = "metrics";
    private static final String SECTION_API = "api";
    private static final String SECTION_OAUTH2 = "oauth2";
    private static final String SECTION_I18N= "i18n";
    private static final String SECTION_MARKUP = "markup";
    private static final String SECTION_HIGHLIGHT_MAPPING = "highlight.mapping";
    private static final String SECTION_TIME = "time";
    private static final String SECTION_TASK = "task";
    private static final String SECTION_FEDERATION = "federation";
    private static final String SECTION_PACKAGES = "packages";
    private static final String SECTION_MIRROR = "mirror";
    private static final String SECTION_LFS = "lfs";
    private static final String SECTION_STORAGE = "storage";
    private static final String SECTION_STORAGE_REPO_ARCHIVE = "storage.repo-archive";
    private static final String SECTION_REPO_ARCHIVE = "repo-archive";
    private static final String SECTION_PROXY = "proxy";
    private static final String SECTION_ACTIONS = "actions";
    private static final String SECTION_OTHER = "other";
	private static final String SECTION_LOG = "log";
	private static final String LABEL_KEY = "devjoy.io/configsecret.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
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
		client.apps().deployments().inNamespace(actual.getMetadata().getNamespace())
				.withName(actual.getMetadata().getOwnerReferences().get(0).getName()).rolling().restart();
		return updated;
	  }
	
	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea config map");
		Secret cm = client
				.secrets()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/config-secret.yaml"))
				.item();
		
		String name = primary.getMetadata().getName() + "-config";
		cm.getMetadata().setName(name);
		cm.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (cm.getMetadata().getLabels() == null) {
			cm.getMetadata().setLabels(new HashMap<>());
		}
		cm.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		if (cm.getMetadata().getAnnotations() == null) {
			cm.getMetadata().setAnnotations(new HashMap<>());
		}
		String iniData = cm.getStringData().get("app.ini");
		INIConfiguration iniConfiguration = new INIConfiguration();
		LOG.debug("Reading app.ini initial data {}", iniData);
		try (StringReader fileReader = new StringReader(iniData)) {
		    iniConfiguration.read(fileReader);
		    LOG.info("Read app.ini. Adding configuration parameters based on Gitea resource.");
		    iniConfiguration.setProperty("APP_NAME", primary.getMetadata().getName());
		    configureDatabase(primary, iniConfiguration);
		    if(primary.getSpec().isIngressEnabled() && ocpClient.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)){
				Optional<Route> route = Optional.ofNullable(GiteaRouteDependentResource.getResource(primary, ocpClient)
					.waitUntilCondition(c -> c!= null && !StringUtil.isNullOrEmpty(c.getSpec().getHost()), 30, TimeUnit.SECONDS));
				route.ifPresent(r -> configureRoute(iniConfiguration, r, primary.getSpec().isSsl()));
			}
		    configureServer(iniConfiguration);
		    iniConfiguration.getSection(SECTION_MIGRATIONS).setProperty("ALLOW_LOCALNETWORKS", "false");
		    if (primary.getSpec().getLogLevel() != null) {
		    	iniConfiguration.getSection(SECTION_LOG).setProperty("LEVEL", primary.getSpec().getLogLevel());
		    }
		    if (primary.getSpec().getMailer() != null) {
		    	configureMailer(iniConfiguration, primary.getSpec().getMailer());
		    }
		    configureService(primary, iniConfiguration);
			addOverrides(primary, iniConfiguration);
		    StringWriter iniWriter = new StringWriter();
		    iniConfiguration.write(iniWriter);
		    String finalAppIni = iniWriter.toString();
		    LOG.debug("app.ini after adding configuration parameters {}", finalAppIni);
		    //cm.getStringData().put("app.ini", finalAppIni);
			cm.getStringData().clear();
			cm.setData(new HashMap<>());
			cm.getData().put("app.ini", new String(Base64.getEncoder().encode(
				finalAppIni.getBytes())));
		} catch (ConfigurationException | IOException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}

	private void addOverrides(Gitea primary, INIConfiguration iniConfiguration) {
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
        configOverrides.getUiMeta().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_META).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiNotification().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_NOTIFICATION).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiSvg().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_SVG).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getUiCsv().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_UI_CSV).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMarkdown().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MARKDOWN).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getServer().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_SERVER).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getDatabase().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_DATABASE).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getIndexer().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_INDEX).setProperty(e.getKey().toUpperCase(), e.getValue()));
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
        configOverrides.getCronUpdateMigrationPosterId().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_MIGRATION_POSTER_ID).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getCronSyncExternalUsers().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_CRON_SYNC_EXTERNAL_USERS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getGit().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getGitConfig().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT_CONFIG).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getGitTimeout().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_GIT_TIMEOUT).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMetrics().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_METRICS).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getApi().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_API).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getOauth2().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_OAUTH2).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getI18n().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_I18N).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getMarkup().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_MARKUP).setProperty(e.getKey().toUpperCase(), e.getValue()));
        configOverrides.getHighlightMapping().entrySet().forEach( e -> iniConfiguration.getSection(SECTION_HIGHLIGHT_MAPPING).setProperty(e.getKey().toUpperCase(), e.getValue()));
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


	private void configureService(Gitea primary, INIConfiguration iniConfiguration) {
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("REGISTER_EMAIL_CONFIRM", primary.getSpec().isRegisterEmailConfirm());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_NOTIFY_MAIL", primary.getSpec().getMailer().isEnableNotifyMail());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DISABLE_REGISTRATION", primary.getSpec().isDisableRegistration());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_CAPTCHA", primary.getSpec().isEnableCaptcha());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DEFAULT_ALLOW_CREATE_ORGANIZATION", primary.getSpec().isAllowCreateOrganization());
	}
	private void configureMailer(INIConfiguration iniConfiguration, GiteaMailerSpec mailer) {
		iniConfiguration.getSection(SECTION_MAILER).setProperty("ENABLED", mailer.isEnabled());
		if (mailer.isEnabled()) {
			iniConfiguration.getSection(SECTION_MAILER).setProperty("FROM", mailer.getFrom());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("PROTOCOL", mailer.getType());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("SMTP_ADDR", mailer.getHost());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("USER", mailer.getUser());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("PASSWD", mailer.getPassword());
			if (StringUtil.isNullOrEmpty(mailer.getHeloHostname())) {
				iniConfiguration.getSection(SECTION_MAILER).setProperty("HELO_HOSTNAME", mailer.getHeloHostname());
			}
		}
	}
	private void configureServer(INIConfiguration iniConfiguration) {
		iniConfiguration.getSection(SECTION_SERVER).setProperty("HTTP_PORT", "3000");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_PORT", "2022");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_PORT", "2022");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("DISABLE_SSH", "true");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("START_SSH_SERVER", "false");
	}
	private void configureRoute(INIConfiguration iniConfiguration, Route r, boolean ssl) {
		String protocol = "http" + (ssl ? "s://" : "://");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("ROOT_URL", protocol + r.getSpec().getHost() + "/");
		iniConfiguration.getSection(SECTION_SERVER).setProperty("SSH_DOMAIN", r.getSpec().getHost());
		iniConfiguration.getSection(SECTION_SERVER).setProperty("DOMAIN", r.getSpec().getHost());
	}
	private void configureDatabase(Gitea primary, INIConfiguration iniConfiguration) {
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("HOST", "postgresql-" + primary.getMetadata().getName() +":5432");
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("NAME", config.getDatabaseName());
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("USER", config.getUserName());
		iniConfiguration.getSection(SECTION_DATABASE).setProperty("PASSWD", config.getPassword());
	}
}
