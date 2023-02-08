package io.devjoy.gitea.k8s.gitea;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaMailerSpec;
import io.devjoy.gitea.k8s.postgres.PostgresConfig;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(labelSelector = GiteaConfigMapDependentResource.LABEL_SELECTOR)
public class GiteaConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Gitea>{
	private static final Logger LOG = LoggerFactory.getLogger(GiteaConfigMapDependentResource.class);
	private static final String SECTION_DATABASE = "database";
	private static final String SECTION_MIGRATIONS = "migrations";
	private static final String SECTION_SERVER = "server";
	private static final String SECTION_MAILER = "mailer";
	private static final String SECTION_SERVICE = "service";
	private static final String SECTION_LOG = "log";
	private static final String LABEL_KEY = "devjoy.io/cm.target";
	private static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@Inject
	PostgresConfig config;
	@Inject
	OpenShiftClient ocpClient;
	
	public GiteaConfigMapDependentResource() {
		super(ConfigMap.class);
	}
	
	@Override
	protected void onUpdated(ResourceID primaryResourceId, ConfigMap updated, ConfigMap actual) {
		super.onUpdated(primaryResourceId, updated, actual);
		client.apps().deployments().inNamespace(updated.getMetadata().getNamespace())
				.withName(updated.getMetadata().getOwnerReferences().get(0).getName()).rolling().restart();

	}
	
	@Override
	protected ConfigMap desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea config map");
		ConfigMap cm = client
				.configMaps()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/config-map.yaml"))
				.get();
		
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
		String iniData = cm.getData().get("app.ini");
		INIConfiguration iniConfiguration = new INIConfiguration();
		LOG.debug("Reading app.ini initial data {}", iniData);
		try (StringReader fileReader = new StringReader(iniData)) {
		    iniConfiguration.read(fileReader);
		    LOG.info("Read app.ini. Adding configuration paramters based on Gitea resource.");
		    iniConfiguration.setProperty("APP_NAME", primary.getMetadata().getName());
		    configureDatabase(primary, iniConfiguration);
		    Optional<Route> route = Optional.ofNullable(GiteaRouteDependentResource.getResource(primary, ocpClient)
		    	.waitUntilCondition(c -> c!= null && !StringUtil.isNullOrEmpty(c.getSpec().getHost()), 30, TimeUnit.SECONDS));
		    route.ifPresent(r -> configureRoute(iniConfiguration, r, primary.getSpec().isSsl()));
		    configureServer(iniConfiguration);
		    iniConfiguration.getSection(SECTION_MIGRATIONS).setProperty("ALLOW_LOCALNETWORKS", "false");
		    if (primary.getSpec().getLogLevel() != null) {
		    	iniConfiguration.getSection(SECTION_LOG).setProperty("LEVEL", primary.getSpec().getLogLevel());
		    }
		    if (primary.getSpec().getMailer() != null) {
		    	configureMailer(iniConfiguration, primary.getSpec().getMailer());
		    }
		    configureService(primary, iniConfiguration);
		    StringWriter iniWriter = new StringWriter();
		    iniConfiguration.write(iniWriter);
		    String finalAppIni = iniWriter.toString();
		    LOG.debug("app.ini after dding configuration paramters {}", finalAppIni);
		    cm.getData().put("app.ini", finalAppIni);
		} catch (ConfigurationException | IOException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}
	private void configureService(Gitea primary, INIConfiguration iniConfiguration) {
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("REGISTER_EMAIL_CONFIRM", primary.getSpec().isRegisterEmailConfirm());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_NOTIFY_MAIL", primary.getSpec().isEnableNotifyMail());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DISABLE_REGISTRATION", primary.getSpec().isDisableRegistration());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("ENABLE_CAPTCHA", primary.getSpec().isEnableCaptcha());
		iniConfiguration.getSection(SECTION_SERVICE).setProperty("DEFAULT_ALLOW_CREATE_ORGANIZATION", primary.getSpec().isAllowCreateOrganization());
	}
	private void configureMailer(INIConfiguration iniConfiguration, GiteaMailerSpec mailer) {
		iniConfiguration.getSection(SECTION_MAILER).setProperty("ENABLED", mailer.isEnabled());
		if (mailer.isEnabled()) {
			iniConfiguration.getSection(SECTION_MAILER).setProperty("FROM", mailer.isEnabled());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("MAILER_TYPE", mailer.getType());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("HOST", mailer.getHost());
			iniConfiguration.getSection(SECTION_MAILER).setProperty("IS_TLS_ENABLED", mailer.isTls());
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
