package io.devjoy.operator.environment.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class EnvironmentServiceImpl {
	private static final Logger LOG = LoggerFactory.getLogger(EnvironmentServiceImpl.class);
	@Inject
	Config config;
	
	private final KubernetesClient client;
	
	public EnvironmentServiceImpl(KubernetesClient client) {
		this.client = client;
	}
	

}
