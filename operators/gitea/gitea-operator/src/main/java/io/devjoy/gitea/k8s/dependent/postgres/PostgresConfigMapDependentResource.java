package io.devjoy.gitea.k8s.dependent.postgres;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = PostgresConfigMapDependentResource.LABEL_SELECTOR)
public class PostgresConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Gitea>{
	private static final String LABEL_KEY = "devjoy.io/configmap.type";
	private static final String LABEL_VALUE = "gitea-postgres";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
    public static final String MOUNT_PATH_CERTS = "/opt/app-root/src/certificates/"; 
	
	public PostgresConfigMapDependentResource() {
		super(ConfigMap.class);
	}
	@Override
	protected ConfigMap desired(Gitea primary, Context<Gitea> context) {
		
        Map<String, String> data = new HashMap<>();
        Map<String, String> postgresConfig = new HashMap<>();
        if (primary.getSpec().getPostgres().isSsl()) {
            postgresConfig.put("ssl", "on");
            postgresConfig.put("ssl_cert_file", "'" + MOUNT_PATH_CERTS + "tls.crt'");
            postgresConfig.put("ssl_key_file", "'" + MOUNT_PATH_CERTS + "tls.key'");
            data.put("custom.conf", postgresConfig.entrySet().stream().map(e -> e.getKey() + " = " + e.getValue()).collect(Collectors.joining("\n")));
        }
        return new ConfigMapBuilder()
            .withNewMetadata()
                .withName(getName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .addToLabels(LABEL_KEY, LABEL_VALUE)
            .endMetadata()
            .withData(data)
            .build();
	}

    public static String getName(Gitea primary) {
        return "postgres-config-" + primary.getMetadata().getName();
    }

}