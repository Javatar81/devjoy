package io.devjoy.operator.project.k8s;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("devjoy.io")
public class Project extends CustomResource<ProjectSpec, ProjectStatus> implements Namespaced {

	private static final long serialVersionUID = -8776278057613557829L;
	
	@JsonIgnore
	public Optional<DevEnvironment> getOwningEnvironment(KubernetesClient client) {
		return Optional.ofNullable(
				client.resources(DevEnvironment.class).inNamespace(this.getSpec().getEnvironmentNamespace())
						.withName(this.getSpec().getEnvironmentName()).get());
	}
}

