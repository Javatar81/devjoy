package io.devjoy.operator.environment.k8s.deploy;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(resourceDiscriminator = AdditionalDeployResourcesConfigmapDiscriminator.class, labelSelector = AdditionalDeployResourcesConfigmapDependentResource.LABEL_SELECTOR)
public class AdditionalDeployResourcesConfigmapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, DevEnvironment>{
	private static final String LABEL_KEY = "devjoy.io/configmap.type";
	private static final String LABEL_VALUE = "additionalresources-deploy";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	
	public AdditionalDeployResourcesConfigmapDependentResource() {
		super(ConfigMap.class);
	}
	@Override
	protected ConfigMap desired(DevEnvironment primary, Context<DevEnvironment> context) {
		ConfigMap cm = context.getClient()
				.configMaps()
				.load(getClass().getClassLoader().getResourceAsStream("deploy/additional-resources-deploy-cm.yaml"))
				.item();
		
		String name = getName(primary);
		cm.getMetadata().setName(name);
		cm.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (cm.getMetadata().getLabels() == null) {
			cm.getMetadata().setLabels(new HashMap<>());
		}
		cm.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		try {
			if (cm.getData() == null) {
				cm.setData(new HashMap<>());
			}
			String devFileContent = Files.readString(Path.of(getClass().getClassLoader().getResource("deploy/argo-application.yaml").toURI()));
			cm.getData().put("argo-application.yaml", devFileContent);
			String helmValues = Files.readString(Path.of(getClass().getClassLoader().getResource("deploy/helm-values-test.yaml").toURI()));
			cm.getData().put("values.yaml", helmValues);
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}

	public static String getName(DevEnvironment primary) {
		return "additional-resources-deploy-" + primary.getMetadata().getName();
	}
	
}