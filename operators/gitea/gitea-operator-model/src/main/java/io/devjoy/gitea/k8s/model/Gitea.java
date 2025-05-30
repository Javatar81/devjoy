package io.devjoy.gitea.k8s.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version(Gitea.API_VERSION)
@Group(Gitea.API_GROUP)
public class Gitea extends CustomResource<GiteaSpec, GiteaStatus> implements Namespaced {
	public static final String API_GROUP = "devjoy.io";
	public static final String API_VERSION = "v1alpha1";
	private static final long serialVersionUID = -6663671542664201741L;
	
}

