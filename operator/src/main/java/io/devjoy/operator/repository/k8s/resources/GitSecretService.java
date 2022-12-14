package io.devjoy.operator.repository.k8s.resources;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.KubernetesClient;

@ApplicationScoped
public class GitSecretService {

	private final KubernetesClient client;

	public GitSecretService(KubernetesClient client) {
		super();
		this.client = client;
	}
	
	public Optional<Secret> getByUser(String namespace, String user) {
		return Optional.ofNullable(client.secrets().inNamespace(namespace).withName(String.format("%s-git-secret", user.toLowerCase())).get());
	}
	
}
