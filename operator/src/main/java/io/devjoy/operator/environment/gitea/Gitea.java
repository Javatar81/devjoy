package io.devjoy.operator.environment.gitea;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1")
@Group("gpte.opentlc.com")
public class Gitea extends CustomResource<GiteaSpec, GiteaStatus> implements Namespaced {
	private static final long serialVersionUID = -3140316562080802842L;
}
