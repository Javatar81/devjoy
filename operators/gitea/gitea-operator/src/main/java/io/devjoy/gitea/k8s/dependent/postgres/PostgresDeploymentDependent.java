package io.devjoy.gitea.k8s.dependent.postgres;

import java.util.HashMap;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.OpenShiftActivationCondition;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.postgres.PostgresSpec;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(informer = @Informer(labelSelector = PostgresDeploymentDependent.LABEL_SELECTOR))
public class PostgresDeploymentDependent extends CRUDKubernetesDependentResource<Deployment, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(PostgresDeploymentDependent.class);
	static final String LABEL_KEY = "devjoy.io/deployment.target";
	static final String LABEL_VALUE = "postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@ConfigProperty(name = "io.devjoy.gitea.postgres.image.ocp") 
	String imageOcp;
	@ConfigProperty(name = "io.devjoy.gitea.postgres.image.k8s") 
	String imageK8s;


	public PostgresDeploymentDependent() {
		super(Deployment.class);
	}

	public static String getName(Gitea primary) {
		return "postgresql-" + primary.getMetadata().getName();
	}

	@Override
	protected Deployment desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Postgres deployment");
		Deployment deployment = context.getClient().apps().deployments()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/postgres/deployment.yaml"))
				.item();
		String name = getName(primary);
		deployment.getMetadata().setName(name);
		deployment.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		deployment.getSpec().getSelector().getMatchLabels().put("name", name);
		PodTemplateSpec template = deployment.getSpec().getTemplate();
		template.getMetadata().getLabels().put("name", name);
		Optional<Container> postgresContainer = template.getSpec().getContainers().stream()
				.filter(c -> "postgresql".equals(c.getName())).findFirst();
		postgresContainer.ifPresent(c -> {
			setEnv(PostgresSecretDependent.getName(primary), c);
			if (primary.getSpec() != null && primary.getSpec().getPostgres() != null
					&& primary.getSpec().getPostgres().getManagedConfig() != null) {
				setImage(primary.getSpec().getPostgres(), c, context.getClient());
				if (primary.getSpec().isResourceRequirementsEnabled()) {
					setResources(primary.getSpec().getPostgres(), c);
				} else {
					c.getResources().getRequests().clear();
					c.getResources().getLimits().clear();
				}
			} else {
				c.getResources().getRequests().clear();
				c.getResources().getLimits().clear();
			}

			if (primary.getSpec() != null
					&& primary.getSpec().getPostgres() != null
					&& primary.getSpec().getPostgres().getManagedConfig() != null
					&& primary.getSpec().getPostgres().getManagedConfig().isSsl()) {
				c.getVolumeMounts().add(new VolumeMountBuilder().withName("tls-secret")
						.withMountPath(PostgresConfigMapDependent.MOUNT_PATH_CERTS).build());
				c.getVolumeMounts().add(new VolumeMountBuilder().withName("psql-config")
						.withMountPath("/opt/app-root/src/postgresql-cfg").build());
			}
		});
		template.getSpec().getVolumes().stream()
				.filter(v -> "postgresql-data".equals(v.getName()))
				.findAny().ifPresent(v -> v.getPersistentVolumeClaim().setClaimName(name + "-pvc"));
		if (deployment.getMetadata().getLabels() == null) {
			deployment.getMetadata().setLabels(new HashMap<>());
		}
		if (primary.getSpec() != null && primary.getSpec().getPostgres() != null
				&& primary.getSpec().getPostgres().getManagedConfig() != null
				&& primary.getSpec().getPostgres().getManagedConfig().isSsl()) {
			template.getSpec().getVolumes().add(
					new VolumeBuilder()
							.withName("tls-secret")
							.withNewSecret()
							.withSecretName(PostgresServiceDependent.getServiceCertSecretName(primary))
							.withDefaultMode(0600)
							.endSecret()
							.build());
			template.getSpec().getVolumes().add(
					new VolumeBuilder()
							.withName("psql-config")
							.withNewConfigMap()
							.withName(PostgresConfigMapDependent.getName(primary))
							.endConfigMap()
							.build());
		}
		deployment.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		deployment.getMetadata().getLabels().put("app.kubernetes.io/part-of", primary.getMetadata().getName());
		return deployment;
	}

	private void setEnv(String name, Container c) {
		c.getEnv().stream()
				.filter(e -> "POSTGRESQL_USER".equals(e.getName())).findAny()
				.ifPresent(usr -> usr.getValueFrom().getSecretKeyRef().setName(name));
		c.getEnv().stream()
				.filter(e -> "POSTGRESQL_PASSWORD".equals(e.getName())).findAny()
				.ifPresent(usr -> usr.getValueFrom().getSecretKeyRef().setName(name));
		c.getEnv().stream()
				.filter(e -> "POSTGRESQL_DATABASE".equals(e.getName())).findAny()
				.ifPresent(usr -> usr.getValueFrom().getSecretKeyRef().setName(name));
	}

	private void setResources(PostgresSpec spec, Container c) {
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getCpuRequest())) {
			LOG.info("Setting cpu requests to {} ", spec.getManagedConfig().getCpuRequest());
			c.getResources().getRequests().put("cpu", new Quantity(spec.getManagedConfig().getCpuRequest()));
		}
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getMemoryRequest())) {
			c.getResources().getRequests().put("memory", new Quantity(spec.getManagedConfig().getMemoryRequest()));
		}
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getCpuLimit())) {
			c.getResources().getLimits().put("cpu", new Quantity(spec.getManagedConfig().getCpuLimit()));
		}
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getMemoryLimit())) {
			c.getResources().getLimits().put("memory", new Quantity(spec.getManagedConfig().getMemoryLimit()));
		}
	}

	private void setImage(PostgresSpec spec, Container c, KubernetesClient client) {
		String[] imageAndTag = c.getImage().split(":");
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getImage())) {
			imageAndTag[0] = spec.getManagedConfig().getImage();
		} else {
			if (OpenShiftActivationCondition.serverSupportsApi(client)) {
				spec.getManagedConfig().setImage(imageOcp);
			} else {
				spec.getManagedConfig().setImage(imageK8s);
			}
		}
		if (!StringUtil.isNullOrEmpty(spec.getManagedConfig().getImageTag())) {
			imageAndTag[1] = spec.getManagedConfig().getImageTag();
		}
		c.setImage(imageAndTag[0] + ":" + imageAndTag[1]);
	}

}
