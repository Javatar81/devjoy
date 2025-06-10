package io.devjoy.gitea.service;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.repository.k8s.GiteaRepositoryReconciler;
import io.devjoy.gitea.util.ApiAccessMode;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GiteaApiService {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaApiService.class);
	
	
	ApiAccessMode accessMode;

	private final OpenShiftClient client;
	


	public GiteaApiService(OpenShiftClient client,@ConfigProperty(name = "io.devjoy.gitea.api.access.mode") ApiAccessMode accessMode) {
		this.client = client;
		this.accessMode = accessMode;
	}

	public void setAccessMode(ApiAccessMode accessMode) {
		this.accessMode = accessMode;
	}



	public Optional<String> getBaseUri(Gitea gitea) {
		LOG.debug("Determine {} base uri", accessMode);
		if (accessMode == ApiAccessMode.EXTERNAL && (gitea.getSpec() == null || gitea.getSpec().isIngressEnabled()) && client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)) {
			return getRouterBaseUri(gitea);
		} else {
			return Optional.ofNullable(getLocalBaseUri(gitea));
		}
	}
	
	public Optional<String> getRouterBaseUri(Gitea gitea) {
		Optional<Route> route = Optional.ofNullable(GiteaRouteDependent.getResource(gitea, client).get());
		return route.map(r -> getRouterBaseUri(gitea, r));
	}
	
	/**
	 * Use for cluster-external testing 
	 * @param resource
	 * @param r
	 * @return
	 */
	private String getRouterBaseUri(Gitea resource, Route r) {
		String protocol = "http" + (resource.getSpec() != null && resource.getSpec().isSsl() ? "s://" : "://");
		return protocol + r.getSpec().getHost();
	}

	/**
	 * Use when reconciler runs in OCP 
	 * @param resource
	 * @return
	 */
	public String getLocalBaseUri(Gitea resource) {
		return String.format("http://%s.%s.svc.cluster.local:3000", GiteaServiceDependent.getName(resource), resource.getMetadata().getNamespace());
	}
}
