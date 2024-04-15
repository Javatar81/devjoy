package io.devjoy.gitea.k8s.domain;

public class GiteaLabels {
	
	private GiteaLabels() {}
	public static final String LABEL_GITEA_NAMESPACE = "devjoy.io/gitea.namespace";
	public static final String LABEL_GITEA_NAME = "devjoy.io/gitea.name";
}
