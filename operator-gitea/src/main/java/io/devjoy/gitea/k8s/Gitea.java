package io.devjoy.gitea.k8s;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("devjoy.io")
public class Gitea extends CustomResource<GiteaSpec, GiteaStatus> implements Namespaced {

	private static final long serialVersionUID = -6663671542664201741L;}

