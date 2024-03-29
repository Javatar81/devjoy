package io.devjoy.gitea.k8s.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class GiteaConfigOverrides {
    
    @JsonProperty("repository.editor")
    @JsonPropertyDescription("More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---editor-repositoryeditor")
    private Map<String, String> repositoryEditor = new HashMap<>();
    @JsonProperty("repository.pull-request")
    @JsonPropertyDescription("More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---pull-request-repositorypull-request")
    private Map<String, String> repositoryPullRequest = new HashMap<>();
    @JsonProperty("default")
    @JsonPropertyDescription("Overrides properties in the [default] section of app.ini. These values are environment-dependent but form the basis of a lot of values. They will be reported as part of the default configuration when running gitea --help or on start-up. The order they are emitted there is slightly different but we will list them here in the order they are set-up. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#overall-default")
    private Map<String, String> defaults = new HashMap<>();
    @JsonProperty("repository")
    @JsonPropertyDescription("Overrides properties in the [repository] section of app.ini. These values are environment-dependent but form the basis of a lot of values. They will be reported as part of the default configuration when running gitea --help or on start-up. The order they are emitted there is slightly different but we will list them here in the order they are set-up. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository-repository")
    private Map<String, String> repository = new HashMap<>();
    @JsonProperty("repository.issue")
    @JsonPropertyDescription("Overrides properties in the [repository.issue] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---issue-repositoryissue")
    private Map<String, String> repositoryIssue = new HashMap<>();
    @JsonProperty("repository.upload")
    @JsonPropertyDescription("Overrides properties in the [repository.upload] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---upload-repositoryupload")
    private Map<String, String> repositoryUpload = new HashMap<>();
    @JsonProperty("repository.release")
    @JsonPropertyDescription("Overrides properties in the [repository.release] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---release-repositoryrelease")
    private Map<String, String> repositoryRelease = new HashMap<>();
    @JsonProperty("repository.signing")
    @JsonPropertyDescription("Overrides properties in the [repository.signing] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---signing-repositorysigning")
    private Map<String, String> repositorySigning = new HashMap<>();
    @JsonProperty("repository.local")
    @JsonPropertyDescription("Overrides properties in the [repository.local] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository---local-repositorylocal")
    private Map<String, String> repositoryLocal = new HashMap<>();
    @JsonProperty("repository.mimetype_mapping")
    @JsonPropertyDescription("Overrides properties in the [repository.mimetype_mapping] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository----mime-type-mapping-repositorymimetype_mapping")
    private Map<String, String> repositoryMimeTypeMapping = new HashMap<>();
    @JsonProperty("cors")
    @JsonPropertyDescription("Overrides properties in the [cors] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#cors-cors")
    private Map<String, String> cors = new HashMap<>();
    @JsonProperty("ui")
    @JsonPropertyDescription("Overrides properties in the [ui] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui-ui")
    private Map<String, String> ui = new HashMap<>();
    @JsonProperty("ui.admin")
    @JsonPropertyDescription("Overrides properties in the [ui.admin] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---admin-uiadmin")
    private Map<String, String> uiAdmin = new HashMap<>();
    @JsonProperty("ui.user")
    @JsonPropertyDescription("Overrides properties in the [ui.user] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---user-uiuser")
    private Map<String, String> uiUser = new HashMap<>();
    @JsonProperty("ui.meta")
    @JsonPropertyDescription("Overrides properties in the [ui.meta] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---metadata-uimeta")
    private Map<String, String> uiMeta = new HashMap<>();
    @JsonProperty("ui.notification")
    @JsonPropertyDescription("Overrides properties in the [ui.notification] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---notification-uinotification")
    private Map<String, String> uiNotification = new HashMap<>();
    @JsonProperty("ui.svg")
    @JsonPropertyDescription("Overrides properties in the [ui.svg] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---svg-images-uisvg")
    private Map<String, String> uiSvg = new HashMap<>();
    @JsonProperty("ui.csv")
    @JsonPropertyDescription("Overrides properties in the [ui.csv] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ui---csv-files-uicsv")
    private Map<String, String> uiCsv = new HashMap<>();
    @JsonProperty("markdown")
    @JsonPropertyDescription("Overrides properties in the [markdown] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#markdown-markdown")
    private Map<String, String> markdown = new HashMap<>();
    @JsonProperty("server")
    @JsonPropertyDescription("Overrides properties in the [server] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#server-server")
    private Map<String, String> server = new HashMap<>();
    @JsonProperty("database")
    @JsonPropertyDescription("Overrides properties in the [database] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#database-database")
    private Map<String, String> database = new HashMap<>();
    @JsonProperty("indexer")
    @JsonPropertyDescription("Overrides properties in the [indexer] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#indexer-indexer")
    private Map<String, String> indexer = new HashMap<>();
    @JsonProperty("queue")
    @JsonPropertyDescription("Overrides properties in the [queue] section of app.ini. Configuration at [queue] will set defaults for queues with overrides for individual queues at [queue.*]. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#queue-queue-and-queue")
    private Map<String, String> queue = new HashMap<>();
    @JsonProperty("admin")
    @JsonPropertyDescription("Overrides properties in the [admin] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#admin-admin")
    private Map<String, String> admin = new HashMap<>();
    @JsonProperty("security")
    @JsonPropertyDescription("Overrides properties in the [security] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#security-security")
    private Map<String, String> security = new HashMap<>();
    @JsonProperty("camo")
    @JsonPropertyDescription("Overrides properties in the [camo] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#camo-camo")
    private Map<String, String> camo = new HashMap<>();
    @JsonProperty("openid")
    @JsonPropertyDescription("Overrides properties in the [openid] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#openid-openid")
    private Map<String, String> openid = new HashMap<>();
    @JsonProperty("oauth2_client")
    @JsonPropertyDescription("Overrides properties in the [oauth2_client] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#oauth2-client-oauth2_client")
    private Map<String, String> oauth2Client = new HashMap<>();
    @JsonProperty("service")
    @JsonPropertyDescription("Overrides properties in the [service] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#service-service")
    private Map<String, String> service = new HashMap<>();
    @JsonProperty("service.explore")
    @JsonPropertyDescription("Overrides properties in the [service.explore] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#service---explore-serviceexplore")
    private Map<String, String> serviceExplore = new HashMap<>();
    @JsonProperty("ssh.minimum_key_sizes")
    @JsonPropertyDescription("Overrides properties in the [ssh.minimum_key_sizes] section of app.ini. Define allowed algorithms and their minimum key length (use -1 to disable a type): ED25519: 256, ECDSA: 256, RSA: 2047, DSA: -1. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#ssh-minimum-key-sizes-sshminimum_key_sizes")
    private Map<String, String> sshMinimumKeySizes = new HashMap<>();
    @JsonProperty("webhook")
    @JsonPropertyDescription("Overrides properties in the [webhook] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#webhook-webhook")
    private Map<String, String> webhook = new HashMap<>();
    @JsonProperty("mailer")
    @JsonPropertyDescription("Overrides properties in the [mailer] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#mailer-mailer")
    private Map<String, String> mailer = new HashMap<>();
    @JsonProperty("email.incoming")
    @JsonPropertyDescription("Overrides properties in the [email.incoming] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#incoming-email-emailincoming")
    private Map<String, String> emailIncoming = new HashMap<>();
    @JsonProperty("cache")
    @JsonPropertyDescription("Overrides properties in the [cache] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#cache-cache")
    private Map<String, String> cache = new HashMap<>();
    @JsonProperty("cache.last_commit")
    @JsonPropertyDescription("Overrides properties in the [cache.last_commit] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#cache---lastcommitcache-settings-cachelast_commit")
    private Map<String, String> cacheLastCommit = new HashMap<>();
    @JsonProperty("session")
    @JsonPropertyDescription("Overrides properties in the [session] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#session-session")
    private Map<String, String> session = new HashMap<>();
    @JsonProperty("picture")
    @JsonPropertyDescription("Overrides properties in the [picture] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#picture-picture")
    private Map<String, String> picture = new HashMap<>();
    @JsonProperty("project")
    @JsonPropertyDescription("Overrides properties in the [project] section of app.ini. Default templates for project boards. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#project-project")
    private Map<String, String> project = new HashMap<>();
    @JsonProperty("attachment")
    @JsonPropertyDescription("Overrides properties in the [attachment] section of app.ini. Issue and pull request attachments. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#issue-and-pull-request-attachments-attachment")
    private Map<String, String> attachment = new HashMap<>();
    @JsonProperty("log")
    @JsonPropertyDescription("Overrides properties in the [log] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#log-log")
    private Map<String, String> log = new HashMap<>();
    @JsonProperty("log.console")
    @JsonPropertyDescription("Overrides properties in the [log.console] section of app.ini. Console log mode. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#console-log-mode-logconsole-or-modeconsole")
    private Map<String, String> logConsole = new HashMap<>();
    @JsonProperty("log.file")
    @JsonPropertyDescription("Overrides properties in the [log.file] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#file-log-mode-logfile-or-modefile")
    private Map<String, String> logFile = new HashMap<>();
    @JsonProperty("log.conn")
    @JsonPropertyDescription("Overrides properties in the [log.conn] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#conn-log-mode-logconn-or-modeconn")
    private Map<String, String> logConn = new HashMap<>();
    @JsonProperty("cron")
    @JsonPropertyDescription("Overrides properties in the [cron] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#cron-cron")
    private Map<String, String> cron = new HashMap<>();
    @JsonProperty("cron.archive_cleanup")
    @JsonPropertyDescription("Overrides properties in the [cron.archive_cleanup] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronArchiveCleanup = new HashMap<>();
    @JsonProperty("cron.update_mirrors")
    @JsonPropertyDescription("Overrides properties in the [cron.update_mirrors] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronUpdateMirrors = new HashMap<>();
    @JsonProperty("cron.repo_health_check")
    @JsonPropertyDescription("Overrides properties in the [cron.repo_health_check] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronRepoHealthCheck = new HashMap<>();
    @JsonProperty("cron.check_repo_stats")
    @JsonPropertyDescription("Overrides properties in the [cron.check_repo_stats] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronCheckRepoStats = new HashMap<>();
    @JsonProperty("cron.cleanup_hook_task_table")
    @JsonPropertyDescription("Overrides properties in the [cron.cleanup_hook_task_table] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronCleanupHookTaskTable = new HashMap<>();
    @JsonProperty("cron.cleanup_packages")
    @JsonPropertyDescription("Overrides properties in the [cron.cleanup_packages] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronCleanupPackages = new HashMap<>();
    @JsonProperty("cron.update_migration_poster_id")
    @JsonPropertyDescription("Overrides properties in the [cron.update_migration_poster_id] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronUpdateMigrationPosterId = new HashMap<>();
    @JsonProperty("cron.sync_external_users")
    @JsonPropertyDescription("Overrides properties in the [cron.sync_external_users] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#basic-cron-tasks---enabled-by-default")
    private Map<String, String> cronSyncExternalUsers = new HashMap<>();
    @JsonProperty("git")
    @JsonPropertyDescription("Overrides properties in the [git] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#git-git")
    private Map<String, String> git = new HashMap<>();
    @JsonProperty("git.timeout")
    @JsonPropertyDescription("Overrides properties in the [git.timeout] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#git---timeout-settings-gittimeout")
    private Map<String, String> gitTimeout = new HashMap<>();
    @JsonProperty("git.config")
    @JsonPropertyDescription("Overrides properties in the [git.config] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#git---config-options-gitconfign")
    private Map<String, String> gitConfig = new HashMap<>();
    @JsonProperty("metrics")
    @JsonPropertyDescription("Overrides properties in the [metrics] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#metrics-metrics")
    private Map<String, String> metrics = new HashMap<>();
    @JsonProperty("api")
    @JsonPropertyDescription("Overrides properties in the [api] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#api-api")
    private Map<String, String> api = new HashMap<>();
    @JsonProperty("oauth2")
    @JsonPropertyDescription("Overrides properties in the [oauth2] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#oauth2-oauth2")
    private Map<String, String> oauth2 = new HashMap<>();
    @JsonProperty("i18n")
    @JsonPropertyDescription("Overrides properties in the [i18n] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#i18n-i18n")
    private Map<String, String> i18n = new HashMap<>();
    @JsonProperty("markup")
    @JsonPropertyDescription("Overrides properties in the [markup] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#markup-markup")
    private Map<String, String> markup = new HashMap<>();
    @JsonProperty("highlight.mapping")
    @JsonPropertyDescription("Overrides properties in the [highlight.mapping] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#highlight-mappings-highlightmapping")
    private Map<String, String> highlightMapping = new HashMap<>();
    @JsonProperty("time")
    @JsonPropertyDescription("Overrides properties in the [time] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#time-time")
    private Map<String, String> time = new HashMap<>();
    @JsonProperty("task")
    @JsonPropertyDescription("Overrides properties in the [task] section of app.ini. Task queue configuration has been moved to queue.task. However, the  configuration values are kept for backwards compatibility. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#task-task")
    private Map<String, String> task = new HashMap<>();
    @JsonProperty("migrations")
    @JsonPropertyDescription("Overrides properties in the [migrations] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#migrations-migrations")
    private Map<String, String> migrations = new HashMap<>();
    @JsonProperty("federation")
    @JsonPropertyDescription("Overrides properties in the [federation] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#federation-federation")
    private Map<String, String> federation = new HashMap<>();
    @JsonProperty("packages")
    @JsonPropertyDescription("Overrides properties in the [packages] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#packages-packages")
    private Map<String, String> packages = new HashMap<>();
    @JsonProperty("mirror")
    @JsonPropertyDescription("Overrides properties in the [mirror] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#mirror-mirror")
    private Map<String, String> mirror = new HashMap<>();
    @JsonProperty("lfs")
    @JsonPropertyDescription("Overrides properties in the [lfs] section of app.ini. Storage configuration for lfs data. It will be derived from default [storage] or [storage.xxx] when set STORAGE_TYPE to xxx. When derived, the default of PATH is data/lfs and the default of MINIO_BASE_PATH is lfs/. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#lfs-lfs")
    private Map<String, String> lfs = new HashMap<>();
    @JsonProperty("storage")
    @JsonPropertyDescription("Overrides properties in the [storage] section of app.ini. Default storage configuration for attachments, lfs, avatars, repo-avatars, repo-archive, packages, actions_log, actions_artifact. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#storage-storage")
    private Map<String, String> storage = new HashMap<>();
    @JsonProperty("storage.repo-archive")
    @JsonPropertyDescription("Overrides properties in the [storage.repo-archive] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#cron-cron")
    private Map<String, String> storageRepoArchive = new HashMap<>();
    @JsonProperty("repo-archive")
    @JsonPropertyDescription("Overrides properties in the [repo-archive] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#repository-archives-repo-archive")
    private Map<String, String> repoArchive = new HashMap<>();
    @JsonProperty("proxy")
    @JsonPropertyDescription("Overrides properties in the [proxy] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#proxy-proxy")
    private Map<String, String> proxy = new HashMap<>();
    @JsonProperty("actions")
    @JsonPropertyDescription("Overrides properties in the [actions] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#actions-actions")
    private Map<String, String> actions = new HashMap<>();
    @JsonProperty("other")
    @JsonPropertyDescription("Overrides properties in the [other] section of app.ini. More details can be found here: https://docs.gitea.com/administration/config-cheat-sheet#other-other")
    private Map<String, String> other = new HashMap<>();

    public Map<String, String> getRepository() {
        return repository;
    }

    public void setRepository(Map<String, String> repository) {
        this.repository = repository;
    }

    public Map<String, String> getRepositoryEditor() {
        return repositoryEditor;
    }

    public void setRepositoryEditor(Map<String, String> repositoryEditor) {
        this.repositoryEditor = repositoryEditor;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }

    public Map<String, String> getRepositoryPullRequest() {
        return repositoryPullRequest;
    }

    public void setRepositoryPullRequest(Map<String, String> repositoryPullRequest) {
        this.repositoryPullRequest = repositoryPullRequest;
    }

    public Map<String, String> getRepositoryIssue() {
        return repositoryIssue;
    }

    public void setRepositoryIssue(Map<String, String> repositoryIssue) {
        this.repositoryIssue = repositoryIssue;
    }

    public Map<String, String> getRepositoryUpload() {
        return repositoryUpload;
    }

    public void setRepositoryUpload(Map<String, String> repositoryUpload) {
        this.repositoryUpload = repositoryUpload;
    }

    public Map<String, String> getRepositoryRelease() {
        return repositoryRelease;
    }

    public void setRepositoryRelease(Map<String, String> repositoryRelease) {
        this.repositoryRelease = repositoryRelease;
    }

    public Map<String, String> getRepositorySigning() {
        return repositorySigning;
    }

    public void setRepositorySigning(Map<String, String> repositorySigning) {
        this.repositorySigning = repositorySigning;
    }

    public Map<String, String> getRepositoryLocal() {
        return repositoryLocal;
    }

    public void setRepositoryLocal(Map<String, String> repositoryLocal) {
        this.repositoryLocal = repositoryLocal;
    }

    public Map<String, String> getRepositoryMimeTypeMapping() {
        return repositoryMimeTypeMapping;
    }

    public void setRepositoryMimeTypeMapping(Map<String, String> repositoryMimeTypeMapping) {
        this.repositoryMimeTypeMapping = repositoryMimeTypeMapping;
    }

    public Map<String, String> getCors() {
        return cors;
    }

    public void setCors(Map<String, String> cors) {
        this.cors = cors;
    }

    public Map<String, String> getUi() {
        return ui;
    }

    public void setUi(Map<String, String> ui) {
        this.ui = ui;
    }

    public Map<String, String> getUiAdmin() {
        return uiAdmin;
    }

    public void setUiAdmin(Map<String, String> uiAdmin) {
        this.uiAdmin = uiAdmin;
    }

     public Map<String, String> getUiUser() {
        return uiUser;
    }

    public void setUiUser(Map<String, String> uiUser) {
        this.uiUser = uiUser;
    }

    public Map<String, String> getUiMeta() {
        return uiMeta;
    }

    public void setUiMeta(Map<String, String> uiMeta) {
        this.uiMeta = uiMeta;
    }

    public Map<String, String> getUiNotification() {
        return uiNotification;
    }

    public void setUiNotification(Map<String, String> uiNotification) {
        this.uiNotification = uiNotification;
    }

    public Map<String, String> getUiSvg() {
        return uiSvg;
    }

    public void setUiSvg(Map<String, String> uiSvg) {
        this.uiSvg = uiSvg;
    }

    public Map<String, String> getUiCsv() {
        return uiCsv;
    }

    public void setUiCsv(Map<String, String> uiCsv) {
        this.uiCsv = uiCsv;
    }

    public Map<String, String> getMarkdown() {
        return markdown;
    }

    public void setMarkdown(Map<String, String> markdown) {
        this.markdown = markdown;
    }

    public Map<String, String> getServer() {
        return server;
    }

    public void setServer(Map<String, String> server) {
        this.server = server;
    }

    public Map<String, String> getDatabase() {
        return database;
    }

    public void setDatabase(Map<String, String> database) {
        this.database = database;
    }

    public Map<String, String> getIndexer() {
        return indexer;
    }

    public void setIndexer(Map<String, String> indexer) {
        this.indexer = indexer;
    }

    public Map<String, String> getQueue() {
        return queue;
    }

    public void setQueue(Map<String, String> queue) {
        this.queue = queue;
    }

    public Map<String, String> getAdmin() {
        return admin;
    }

    public void setAdmin(Map<String, String> admin) {
        this.admin = admin;
    }

    public Map<String, String> getSecurity() {
        return security;
    }

    public void setSecurity(Map<String, String> security) {
        this.security = security;
    }

    public Map<String, String> getCamo() {
        return camo;
    }

    public void setCamo(Map<String, String> camo) {
        this.camo = camo;
    }

    public Map<String, String> getOpenid() {
        return openid;
    }

    public void setOpenid(Map<String, String> openid) {
        this.openid = openid;
    }

    public Map<String, String> getOauth2Client() {
        return oauth2Client;
    }

    public void setOauth2Client(Map<String, String> oauth2Client) {
        this.oauth2Client = oauth2Client;
    }

    public Map<String, String> getService() {
        return service;
    }

    public void setService(Map<String, String> service) {
        this.service = service;
    }

    public Map<String, String> getServiceExplore() {
        return serviceExplore;
    }

    public void setServiceExplore(Map<String, String> serviceExplore) {
        this.serviceExplore = serviceExplore;
    }

    public Map<String, String> getSshMinimumKeySizes() {
        return sshMinimumKeySizes;
    }

    public void setSshMinimumKeySizes(Map<String, String> sshMinimumKeySizes) {
        this.sshMinimumKeySizes = sshMinimumKeySizes;
    }

    public Map<String, String> getWebhook() {
        return webhook;
    }

    public void setWebhook(Map<String, String> webhook) {
        this.webhook = webhook;
    }

    public Map<String, String> getMailer() {
        return mailer;
    }

    public void setMailer(Map<String, String> mailer) {
        this.mailer = mailer;
    }

    public Map<String, String> getEmailIncoming() {
        return emailIncoming;
    }

    public void setEmailIncoming(Map<String, String> emailIncoming) {
        this.emailIncoming = emailIncoming;
    }

    public Map<String, String> getCache() {
        return cache;
    }

    public void setCache(Map<String, String> cache) {
        this.cache = cache;
    }

    public Map<String, String> getCacheLastCommit() {
        return cacheLastCommit;
    }

    public void setCacheLastCommit(Map<String, String> cacheLastCommit) {
        this.cacheLastCommit = cacheLastCommit;
    }

    public Map<String, String> getSession() {
        return session;
    }

    public void setSession(Map<String, String> session) {
        this.session = session;
    }

    public Map<String, String> getPicture() {
        return picture;
    }

    public void setPicture(Map<String, String> picture) {
        this.picture = picture;
    }

    public Map<String, String> getProject() {
        return project;
    }

    public void setProject(Map<String, String> project) {
        this.project = project;
    }

    public Map<String, String> getAttachment() {
        return attachment;
    }

    public void setAttachment(Map<String, String> attachment) {
        this.attachment = attachment;
    }

    public Map<String, String> getLog() {
        return log;
    }

    public void setLog(Map<String, String> log) {
        this.log = log;
    }

    public Map<String, String> getLogConsole() {
        return logConsole;
    }

    public void setLogConsole(Map<String, String> logConsole) {
        this.logConsole = logConsole;
    }

    public Map<String, String> getLogFile() {
        return logFile;
    }

    public void setLogFile(Map<String, String> logFile) {
        this.logFile = logFile;
    }

    public Map<String, String> getLogConn() {
        return logConn;
    }

    public void setLogConn(Map<String, String> logConn) {
        this.logConn = logConn;
    }

    public Map<String, String> getCron() {
        return cron;
    }

    public void setCron(Map<String, String> cron) {
        this.cron = cron;
    }

    public Map<String, String> getCronArchiveCleanup() {
        return cronArchiveCleanup;
    }

    public void setCronArchiveCleanup(Map<String, String> cronArchiveCleanup) {
        this.cronArchiveCleanup = cronArchiveCleanup;
    }

    public Map<String, String> getCronUpdateMirrors() {
        return cronUpdateMirrors;
    }

    public void setCronUpdateMirrors(Map<String, String> cronUpdateMirrors) {
        this.cronUpdateMirrors = cronUpdateMirrors;
    }

    public Map<String, String> getCronRepoHealthCheck() {
        return cronRepoHealthCheck;
    }

    public void setCronRepoHealthCheck(Map<String, String> cronRepoHealthCheck) {
        this.cronRepoHealthCheck = cronRepoHealthCheck;
    }

    public Map<String, String> getCronCheckRepoStats() {
        return cronCheckRepoStats;
    }

    public void setCronCheckRepoStats(Map<String, String> cronCheckRepoStats) {
        this.cronCheckRepoStats = cronCheckRepoStats;
    }

    public Map<String, String> getCronCleanupHookTaskTable() {
        return cronCleanupHookTaskTable;
    }

    public void setCronCleanupHookTaskTable(Map<String, String> cronCleanupHookTaskTable) {
        this.cronCleanupHookTaskTable = cronCleanupHookTaskTable;
    }

    public Map<String, String> getCronCleanupPackages() {
        return cronCleanupPackages;
    }

    public void setCronCleanupPackages(Map<String, String> cronCleanupPackages) {
        this.cronCleanupPackages = cronCleanupPackages;
    }

    public Map<String, String> getCronUpdateMigrationPosterId() {
        return cronUpdateMigrationPosterId;
    }

    public void setCronUpdateMigrationPosterId(Map<String, String> cronUpdateMigrationPosterId) {
        this.cronUpdateMigrationPosterId = cronUpdateMigrationPosterId;
    }

    public Map<String, String> getCronSyncExternalUsers() {
        return cronSyncExternalUsers;
    }

    public void setCronSyncExternalUsers(Map<String, String> cronSyncExternalUsers) {
        this.cronSyncExternalUsers = cronSyncExternalUsers;
    }

    public Map<String, String> getGit() {
        return git;
    }

    public void setGit(Map<String, String> git) {
        this.git = git;
    }

    public Map<String, String> getGitTimeout() {
        return gitTimeout;
    }

    public void setGitTimeout(Map<String, String> gitTimeout) {
        this.gitTimeout = gitTimeout;
    }

    public Map<String, String> getGitConfig() {
        return gitConfig;
    }

    public void setGitConfig(Map<String, String> gitConfig) {
        this.gitConfig = gitConfig;
    }

    public Map<String, String> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, String> metrics) {
        this.metrics = metrics;
    }

    public Map<String, String> getApi() {
        return api;
    }

    public void setApi(Map<String, String> api) {
        this.api = api;
    }

    public Map<String, String> getOauth2() {
        return oauth2;
    }

    public void setOauth2(Map<String, String> oauth2) {
        this.oauth2 = oauth2;
    }

    public Map<String, String> getI18n() {
        return i18n;
    }

    public void setI18n(Map<String, String> i18n) {
        this.i18n = i18n;
    }

    public Map<String, String> getMarkup() {
        return markup;
    }

    public void setMarkup(Map<String, String> markup) {
        this.markup = markup;
    }

    public Map<String, String> getHighlightMapping() {
        return highlightMapping;
    }

    public void setHighlightMapping(Map<String, String> highlightMapping) {
        this.highlightMapping = highlightMapping;
    }

    public Map<String, String> getTime() {
        return time;
    }

    public void setTime(Map<String, String> time) {
        this.time = time;
    }

    public Map<String, String> getTask() {
        return task;
    }

    public void setTask(Map<String, String> task) {
        this.task = task;
    }

    public Map<String, String> getMigrations() {
        return migrations;
    }

    public void setMigrations(Map<String, String> migrations) {
        this.migrations = migrations;
    }

    public Map<String, String> getFederation() {
        return federation;
    }

    public void setFederation(Map<String, String> federation) {
        this.federation = federation;
    }

    public Map<String, String> getPackages() {
        return packages;
    }

    public void setPackages(Map<String, String> packages) {
        this.packages = packages;
    }

    public Map<String, String> getMirror() {
        return mirror;
    }

    public void setMirror(Map<String, String> mirror) {
        this.mirror = mirror;
    }

    public Map<String, String> getLfs() {
        return lfs;
    }

    public void setLfs(Map<String, String> lfs) {
        this.lfs = lfs;
    }

    public Map<String, String> getStorage() {
        return storage;
    }

    public void setStorage(Map<String, String> storage) {
        this.storage = storage;
    }

    public Map<String, String> getStorageRepoArchive() {
        return storageRepoArchive;
    }

    public void setStorageRepoArchive(Map<String, String> storageRepoArchive) {
        this.storageRepoArchive = storageRepoArchive;
    }

    public Map<String, String> getRepoArchive() {
        return repoArchive;
    }

    public void setRepoArchive(Map<String, String> repoArchive) {
        this.repoArchive = repoArchive;
    }

    public Map<String, String> getProxy() {
        return proxy;
    }

    public void setProxy(Map<String, String> proxy) {
        this.proxy = proxy;
    }

    public Map<String, String> getActions() {
        return actions;
    }

    public void setActions(Map<String, String> actions) {
        this.actions = actions;
    }

    public Map<String, String> getOther() {
        return other;
    }

    public void setOther(Map<String, String> other) {
        this.other = other;
    }

    
}
