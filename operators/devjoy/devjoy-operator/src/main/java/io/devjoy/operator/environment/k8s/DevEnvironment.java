package io.devjoy.operator.environment.k8s;

import com.fasterxml.jackson.annotation.JsonClassDescription;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("devjoy.io")
@JsonClassDescription("This resource represents a development environment.")
public class DevEnvironment extends CustomResource<DevEnvironmentSpec, DevEnvironmentStatus> implements Namespaced {

	private static final long serialVersionUID = -581698667891769286L;
}
