package io.devjoy.gitea.repository.k8s.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.util.StringUtil;

@Version("v1alpha1")
@Group("devjoy.io")
public class GiteaRepository extends CustomResource<GiteaRepositorySpec, GiteaRepositoryStatus> implements Namespaced {

	private static final long serialVersionUID = -8205693419808373306L;
	
	@Override
	public void setSpec(GiteaRepositorySpec spec) {
		// TODO Auto-generated method stub
		super.setSpec(spec);
	}
	
	@JsonIgnore
	public Optional<Gitea> associatedGitea(KubernetesClient client) {
		Map<String, String> labels = getMetadata().getLabels();
		String giteaName = labels.get(GiteaRepositoryLabels.LABEL_GITEA_NAME);
		String giteaNamespace = labels.get(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE);
		if (associatedGiteaLabelsSet(getMetadata())) {
			return Optional
					.ofNullable(client.resources(Gitea.class).inNamespace(giteaNamespace)
							.withName(giteaName).get());
		} else {
			giteaNamespace = !StringUtil.isNullOrEmpty(giteaNamespace)
					? giteaNamespace
					: getMetadata().getNamespace();
			List<Gitea> giteasInSameNamespace = client.resources(Gitea.class).inNamespace(giteaNamespace).list()
					.getItems();
			if (giteasInSameNamespace.size() == 1) {
				Gitea uniqueGiteaInSameNamespace = giteasInSameNamespace.get(0);
				return Optional.of(uniqueGiteaInSameNamespace);
			} else {
				
				throw new GiteaNotFoundException(String.format("Cannot determine unique Gitea in namespace %s. Expected 1 but was %d. Create a Gitea resource before you create a repository.", giteaNamespace, giteasInSameNamespace.size())
				, giteaNamespace);
			}
		}
	}
	
	private boolean associatedGiteaLabelsSet(ObjectMeta meta) {
		return !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaRepositoryLabels.LABEL_GITEA_NAME)) 
			&& !StringUtil.isNullOrEmpty(meta.getLabels().get(GiteaRepositoryLabels.LABEL_GITEA_NAMESPACE));
	}

}

