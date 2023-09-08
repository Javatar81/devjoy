package io.devjoy.gitea.k8s.postgres;

import java.util.Base64;
import java.util.HashMap;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;


@KubernetesDependent(resourceDiscriminator = PostgresSecretDiscriminator.class, labelSelector = PostgresSecretDependentResource.LABEL_SELECTOR)
public class PostgresSecretDependentResource extends CRUDKubernetesDependentResource<Secret, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(PostgresSecretDependentResource.class);
	private static final String LABEL_KEY = "devjoy.io/secret.target";
	private static final String LABEL_VALUE = "postgres";
	public static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	@Inject
	PostgresConfig config;
	
	public PostgresSecretDependentResource() {
		super(Secret.class);
		
	}

	@Override
	protected Secret desired(Gitea primary, Context<Gitea> context) {
		LOG.debug("Setting desired state");
		Secret desired = client.resources(Secret.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/postgres/secret.yaml")).item();
		String name = desired.getMetadata().getName() + primary.getMetadata().getName();
		desired.getMetadata().setName(name);
		desired.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		desired.getData().put("database-name", new String(Base64.getEncoder().encode(
				config.getDatabaseName().getBytes())));
		desired.getData().put("database-user", new String(Base64.getEncoder().encode(
				config.getUserName().getBytes())));
		desired.getData().put("database-password", new String(Base64.getEncoder().encode(
				config.getPassword().getBytes())));
		if (desired.getMetadata().getLabels() == null) {
			desired.getMetadata().setLabels(new HashMap<>());
		}
		desired.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		return desired;
	}
}
