package io.devjoy.operator.project.k8s;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent
public class ConfigMapDependentResource extends CRUDKubernetesDependentResource<ConfigMap, Project>{

	private ObjectMapper mapper = new ObjectMapper(new YAMLFactory());;
	
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
		GiteaRepository repository = client.resources(GiteaRepository.class)
			.inNamespace(primary.getMetadata().getNamespace())
			.withName(primary.getMetadata().getName())
			.waitUntilCondition(r -> r != null && r.getStatus() != null && !StringUtil.isNullOrEmpty(r.getStatus().getCloneUrl()), 1, TimeUnit.MINUTES);
		String cloneUrl = getCloneUrl(primary, repository);
		try {
			if (cm.getData() == null) {
				cm.setData(new HashMap<>());
			}
			String devFileContent = Files.readString(Path.of(getClass().getClassLoader().getResource("init/quarkus-devfile.yaml").toURI()));
			Map<String, Object> devFile = addConfigParamsToDevFileYaml(primary, cloneUrl, devFileContent);
			cm.getData().put("devfile.yaml", mapper.writeValueAsString(devFile));
		} catch (IOException | URISyntaxException e) {
			throw new RuntimeException(e);
		}
		return cm;
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, Object> addConfigParamsToDevFileYaml(Project primary, String cloneUrl, String devFileContent)
			throws JsonProcessingException {
		Map<String, Object> devFile = mapper.readValue(devFileContent , new TypeReference<Map<String, Object>>(){});
		Map<String, Object> metadata = (Map<String, Object>) devFile.get("metadata");
		metadata.put("name", primary.getMetadata().getName());
		List<?> projects = (List<?>) devFile.get("projects");
		Optional<Map<String, Object>> devjoyProject = (Optional<Map<String, Object>>) projects.stream().findFirst();
		devjoyProject.ifPresent(p -> {
			p.put("name", primary.getMetadata().getName());
			Map<String, Object> git = (Map<String, Object>) p.get("git");
			Map<String, Object> remotes = (Map<String, Object>) git.get("remotes");
			remotes.put("origin", cloneUrl);
		});
		return devFile;
	}
	
	private String getCloneUrl(Project primary, GiteaRepository repository) {
		if (StringUtil.isNullOrEmpty(primary.getSpec().getExistingRepositoryCloneUrl())) {
			if (!StringUtil.isNullOrEmpty(repository.getStatus().getInternalCloneUrl())) {
				return repository.getStatus().getInternalCloneUrl();
			} else {
				return repository.getStatus().getCloneUrl();
			}
		} else {
			return primary.getSpec().getExistingRepositoryCloneUrl();
		}
	}
}
