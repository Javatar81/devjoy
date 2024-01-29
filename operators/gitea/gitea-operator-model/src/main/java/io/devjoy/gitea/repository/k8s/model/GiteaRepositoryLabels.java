package io.devjoy.gitea.repository.k8s.model;

public class GiteaRepositoryLabels {
	
	private GiteaRepositoryLabels() {}
	public static final String LABEL_GITEA_NAMESPACE = "devjoy.io/gitea.namespace";
	public static final String LABEL_GITEA_NAME = "devjoy.io/gitea.name";
}
