package io.devjoy.operator.environment.k8s.init;

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

@KubernetesDependent(labelSelector = AdditionalResourcesConfigmapDependentResource.LABEL_SELECTOR)
public class AdditionalResourcesConfigmapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, DevEnvironment>{
	private static final String LABEL_KEY = "devjoy.io/configmap.type";
	private static final String LABEL_VALUE = "additionalresources";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
	
	public AdditionalResourcesConfigmapDependentResource() {
		super(ConfigMap.class);
	}
	@Override
	protected ConfigMap desired(DevEnvironment primary, Context<DevEnvironment> context) {
		ConfigMap cm = client
				.configMaps()
				.load(getClass().getClassLoader().getResourceAsStream("init/additional-resources-cm.yaml"))
				.get();
		
		String name = cm.getMetadata().getName() + primary.getMetadata().getName();
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
			String devFileContent = Files.readString(Path.of(getClass().getClassLoader().getResource("init/quarkus-devfile.yaml").toURI()));
			Map<String, Object> devFile = addConfigParamsToDevFileYaml(primary, devFileContent);
			cm.getData().put("devfile.yaml", mapper.writeValueAsString(devFile));
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}
	
	private Map<String, Object> addConfigParamsToDevFileYaml(DevEnvironment primary, String devFileContent)
			throws JsonProcessingException {
		Map<String, Object> devFile = mapper.readValue(devFileContent , new TypeReference<Map<String, Object>>(){});
		/*Map<String, Object> metadata = (Map<String, Object>) devFile.get("metadata");
		metadata.put("name", primary.getMetadata().getName());
		List<?> projects = (List<?>) devFile.get("projects");
		Optional<Map<String, Object>> devjoyProject = (Optional<Map<String, Object>>) projects.stream().findFirst();
		devjoyProject.ifPresent(p -> {
			p.put("name", primary.getMetadata().getName());
			Map<String, Object> git = (Map<String, Object>) p.get("git");
			Map<String, Object> remotes = (Map<String, Object>) git.get("remotes");
			remotes.put("origin", cloneUrl);
		});*/
		return devFile;
	}
	
}
