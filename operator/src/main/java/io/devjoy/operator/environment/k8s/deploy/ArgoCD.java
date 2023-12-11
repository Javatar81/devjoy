package io.devjoy.operator.environment.k8s.deploy;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.openapi.quarkus.argocd_yaml.model.ArgoCDSpec;
import org.openapi.quarkus.argocd_yaml.model.ArgoCDStatus;

@Version("v1beta1")
@Group("argoproj.io")
public class ArgoCD  extends CustomResource<ArgoCDSpec, ArgoCDStatus> implements Namespaced {
	

	
}