package io.devjoy.operator.environment.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.operator.Config;
import io.fabric8.kubernetes.client.KubernetesClient;

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
