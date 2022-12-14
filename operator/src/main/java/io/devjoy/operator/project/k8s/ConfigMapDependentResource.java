package io.devjoy.operator.project.k8s;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class ConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Project>{

	public ConfigMapDependentResource() {
		super(ConfigMap.class);
	}
	@Override
	protected ConfigMap desired(Project primary, Context<Project> context) {
		ConfigMap cm = client
				.configMaps()
				.load(getClass().getClassLoader().getResourceAsStream("init/additional-resources-cm.yaml"))
				.get();
		
		String name = cm.getMetadata().getName() + primary.getMetadata().getName();
		cm.getMetadata().setName(name);
		cm.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		String devFileContent;
		try {
			if (cm.getData() == null) {
				cm.setData(new HashMap<>());
			}
			devFileContent = Files.readString(Path.of(getClass().getClassLoader().getResource("init/quarkus-devfile.yaml").toURI()));
			cm.getData().put("devfile.yaml", devFileContent);
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}
}
