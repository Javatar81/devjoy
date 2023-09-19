package io.devjoy.gitea.k8s.postgres;

import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.k8s.GiteaPostgresSpec;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = PostgresDeploymentDiscriminator.class, labelSelector = PostgresDeploymentDependentResource.LABEL_SELECTOR)
public class PostgresDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(PostgresDeploymentDependentResource.class);
	static final String LABEL_KEY = "devjoy.io/deployment.target";
	static final String LABEL_VALUE = "postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public PostgresDeploymentDependentResource() {
		super(Deployment.class);
	}
	
	public static String getName(Gitea primary) {
		return "postgresql-" + primary.getMetadata().getName();
	}

	@Override
	protected Deployment desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Postgres deployment");
		Deployment deployment = client.apps().deployments()
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
			setEnv(PostgresSecretDependentResource.getName(primary), c);
			if (primary.getSpec().getPostgres() != null) {
				setImage(primary.getSpec().getPostgres(), c);
				if (primary.getSpec().isResourceRequirementsEnabled()) {
					setResources(primary.getSpec().getPostgres(), c);
				} else {
					c.getResources().getRequests().clear();
					c.getResources().getLimits().clear();
				}
			}
		});
		template.getSpec().getVolumes().stream()
			.filter(v -> "postgresql-data".equals(v.getName()))
			.findAny().ifPresent(v -> v.getPersistentVolumeClaim().setClaimName(name + "-pvc"));
		if (deployment.getMetadata().getLabels() == null) {
			deployment.getMetadata().setLabels(new HashMap<>());
		}
		deployment.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
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

	private void setResources(GiteaPostgresSpec spec, Container c) {
		if (!StringUtil.isNullOrEmpty(spec.getCpuRequest())) {
			LOG.info("Setting cpu requests to {} ", spec.getCpuRequest());
			c.getResources().getRequests().put("cpu", new Quantity(spec.getCpuRequest()));
		} 
		if (!StringUtil.isNullOrEmpty(spec.getMemoryRequest())) {				
			c.getResources().getRequests().put("memory", new Quantity(spec.getMemoryRequest()));
		}
		if (!StringUtil.isNullOrEmpty(spec.getCpuLimit())) {				
			c.getResources().getLimits().put("cpu", new Quantity(spec.getCpuLimit()));
		}
		if (!StringUtil.isNullOrEmpty(spec.getMemoryLimit())) {				
			c.getResources().getLimits().put("memory", new Quantity(spec.getMemoryLimit()));
		}
	}

	private void setImage(GiteaPostgresSpec spec, Container c) {
		String[] imageAndTag = c.getImage().split(":");
		if (!StringUtil.isNullOrEmpty(spec.getImage())) {
			imageAndTag[0] = spec.getImage();	
		}
		if (!StringUtil.isNullOrEmpty(spec.getImageTag())) {
			imageAndTag[1] = spec.getImageTag();	
		}
		c.setImage(imageAndTag[0] + ":" + imageAndTag[1]);
	}

}
