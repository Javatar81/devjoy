package io.devjoy.gitea.organization.k8s.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.devjoy.gitea.k8s.domain.GiteaLabels;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.repository.k8s.model.GiteaNotFoundException;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.util.StringUtil;

@Version("v1alpha1")
@Group("devjoy.io")
public class GiteaOrganization extends CustomResource<GiteaOrganizationSpec, GiteaOrganizationStatus> implements Namespaced {
	private static final long serialVersionUID = 3714578510003972432L;
	private static final Logger LOG = LoggerFactory.getLogger(GiteaOrganization.class);
	
	@JsonIgnore
	public Optional<Gitea> associatedGitea(KubernetesClient client) {
		Map<String, String> labels = getMetadata().getLabels();
		String giteaName = labels.get(GiteaLabels.LABEL_GITEA_NAME);
		String giteaNamespace = labels.get(GiteaLabels.LABEL_GITEA_NAMESPACE);
		if (associatedGiteaLabelsSet(getMetadata())) {
			LOG.debug("Labels found");
			return Optional
					.ofNullable(client.resources(Gitea.class).inNamespace(giteaNamespace)
							.withName(giteaName).get());
		} else {
			giteaNamespace = !StringUtil.isNullOrEmpty(giteaNamespace)
					? giteaNamespace
					: getMetadata().getNamespace();
			LOG.debug("Labels not found. Trying to find exactly on Gitea instance in namespace.");
			List<Gitea> giteasInSameNamespace = client.resources(Gitea.class).inNamespace(giteaNamespace).list()
					.getItems();
			if (giteasInSameNamespace.size() == 1) {
				LOG.debug("Gitea found");
				Gitea uniqueGiteaInSameNamespace = giteasInSameNamespace.get(0);
				return Optional.of(uniqueGiteaInSameNamespace);
			} else {
				
				throw new GiteaNotFoundException(String.format("Cannot determine unique Gitea in namespace %s. Expected 1 but was %d. Create a Gitea resource before you create an organization.", giteaNamespace, giteasInSameNamespace.size())
				, giteaNamespace);
			}
		}
	}
	
	private boolean associatedGiteaLabelsSet(ObjectMeta meta) {
		return !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaLabels.LABEL_GITEA_NAME)) 
			&& !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaLabels.LABEL_GITEA_NAMESPACE));
	}
	
	@Override
	protected GiteaOrganizationSpec initSpec() {
		return new GiteaOrganizationSpec();
	}
}
