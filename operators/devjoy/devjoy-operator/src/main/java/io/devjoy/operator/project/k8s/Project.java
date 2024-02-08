package io.devjoy.operator.project.k8s;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.quarkus.runtime.util.StringUtil;

@Version("v1alpha1")
@Group("devjoy.io")
@JsonClassDescription("This resource represents a development project.")
public class Project extends CustomResource<ProjectSpec, ProjectStatus> implements Namespaced {

	private static final long serialVersionUID = -8776278057613557829L;
	
	@JsonIgnore
	public Optional<DevEnvironment> getOwningEnvironment(KubernetesClient client) {
		Optional<DevEnvironment> env = Optional.empty();
		//TODO Replace Name? No not here
		//TODO Handle if only one is null
		String envNamespace = StringUtil.isNullOrEmpty(this.getSpec().getEnvironmentNamespace()) ? this.getMetadata().getNamespace() : this.getSpec().getEnvironmentNamespace();
		//Test with name == Null
		return Optional.ofNullable(
				 StringUtil.isNullOrEmpty(this.getSpec().getEnvironmentName())? null : 
					client.resources(DevEnvironment.class).inNamespace(envNamespace)
								.withName(this.getSpec().getEnvironmentName()).get())
		.or(() -> {
			KubernetesResourceList<DevEnvironment> envs = client.resources(DevEnvironment.class).inNamespace(this.getMetadata().getNamespace()).list();
			if (!envs.getItems().isEmpty()) {
				DevEnvironment envInProjectNamespace = envs.getItems().get(0);
				//this.getSpec().setEnvironmentNamespace(envNamespace);
				//this.getSpec().setEnvironmentName(envInProjectNamespace.getMetadata().getNamespace());
				return Optional.of(envInProjectNamespace);
			} else {
				return Optional.empty();
			}
		});
	}
}

