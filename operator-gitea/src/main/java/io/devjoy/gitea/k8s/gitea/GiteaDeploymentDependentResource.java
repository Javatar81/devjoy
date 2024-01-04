package io.devjoy.gitea.k8s.gitea;

import java.util.HashMap;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.PodTemplateSpec;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent(resourceDiscriminator = GiteaDeploymentDiscriminator.class, labelSelector = GiteaDeploymentDependentResource.LABEL_SELECTOR)
public class GiteaDeploymentDependentResource extends CRUDKubernetesDependentResource<Deployment, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaDeploymentDependentResource.class);
	static final String LABEL_KEY = "devjoy.io/deployment.target";
	static final String LABEL_VALUE = "gitea";
	static final String LABEL_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
	
	public GiteaDeploymentDependentResource() {
		super(Deployment.class);
	}
	@Override
	protected Deployment desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea deployment");
		Deployment deployment = context.getClient().apps().deployments()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/deployment.yaml"))
				.item();
		String name = primary.getMetadata().getName();
		deployment.getMetadata().setName(name);
		deployment.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		deployment.getSpec().getSelector().getMatchLabels().put("name", name);
		
		PodTemplateSpec template = deployment.getSpec().getTemplate();
		template.getSpec().setServiceAccountName(name);
		template.getMetadata().getLabels().put("name", name);
		
		Optional<Container> giteaContainer = template.getSpec().getContainers().stream()
				.filter(c -> "gitea".equals(c.getName())).findFirst();
			giteaContainer.ifPresent(c -> {
			setImage(primary, c);
			if (primary.getSpec().isResourceRequirementsEnabled()) {
				setResourcesDefaults(primary, c);
			} else {
				c.getResources().getRequests().clear();
				c.getResources().getLimits().clear();

			}
			
		});
		setVolumes(name, template);
		if (deployment.getMetadata().getLabels() == null) {
			deployment.getMetadata().setLabels(new HashMap<>());
		}
		deployment.getMetadata().getLabels().put(LABEL_KEY, LABEL_VALUE);
		deployment.getMetadata().getLabels().put("app.kubernetes.io/part-of", primary.getMetadata().getName()); 
		return deployment;
	}
	private void setVolumes(String name, PodTemplateSpec template) {
		template.getSpec().getVolumes().stream()
			.filter(v -> "gitea-repositories".equals(v.getName()))
			.findAny().ifPresent(v -> v.getPersistentVolumeClaim().setClaimName(name + "-pvc"));
		template.getSpec().getVolumes().stream()
			.filter(v -> "gitea-config".equals(v.getName()))
			.findAny().ifPresent(v -> v.getSecret().setSecretName(name + "-config"));
	}

	private void setResourcesDefaults(Gitea primary, Container c) {
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getCpuRequest())) {
			LOG.info("Setting cpu requests to {} ", primary.getSpec().getCpuRequest());
			c.getResources().getRequests().put("cpu", new Quantity(primary.getSpec().getCpuRequest()));
		} 
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getMemoryRequest())) {				
			c.getResources().getRequests().put("memory", new Quantity(primary.getSpec().getMemoryRequest()));
		}
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getCpuLimit())) {				
			c.getResources().getLimits().put("cpu", new Quantity(primary.getSpec().getCpuLimit()));
		}
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getMemoryLimit())) {				
			c.getResources().getLimits().put("memory", new Quantity(primary.getSpec().getMemoryLimit()));
		}
	}

	private void setImage(Gitea primary, Container c) {
		String[] imageAndTag = c.getImage().split(":");
		LOG.info("Default image is {}", c.getImage());
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getImage())) {
			imageAndTag[0] = primary.getSpec().getImage();	
		}
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getImageTag())) {
			imageAndTag[1] = primary.getSpec().getImageTag();	
		}
		c.setImage(imageAndTag[0] + ":" + imageAndTag[1]);
		
	}
	
	public static Resource<Deployment> getResource(Gitea primary, KubernetesClient client) {
		return client.apps().deployments().inNamespace(primary.getMetadata().getNamespace()).withName(primary.getMetadata().getName());
	}
}
