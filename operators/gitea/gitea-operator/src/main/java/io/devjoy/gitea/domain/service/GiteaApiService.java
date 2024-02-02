package io.devjoy.gitea.domain.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.devjoy.gitea.domain.ApiAccessMode;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependentResource;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependentResource;
import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GiteaApiService {
	@ConfigProperty(name = "io.devjoy.gitea.api.access.mode") 
	ApiAccessMode accessMode;
	private final OpenShiftClient client;
	
	public GiteaApiService(OpenShiftClient client) {
		this.client = client;
	}
	
	public Optional<String> getBaseUri(Gitea gitea) {
		if (accessMode == ApiAccessMode.EXTERNAL && (gitea.getSpec() == null || gitea.getSpec().isIngressEnabled()) && client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)) {
			return getRouterBaseUri(gitea);
		} else {
			return Optional.ofNullable(getLocalBaseUri(gitea));
		}
	}
	
	public Optional<String> getRouterBaseUri(Gitea gitea) {
		Optional<Route> route = Optional.ofNullable(GiteaRouteDependentResource.getResource(gitea, client)
		    	.waitUntilCondition(c -> c != null && !StringUtil.isNullOrEmpty(c.getSpec().getHost()), 30, TimeUnit.SECONDS));
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
		return String.format("http://%s.%s.svc.cluster.local:3000", GiteaServiceDependentResource.getName(resource), resource.getMetadata().getNamespace());
	}
}
