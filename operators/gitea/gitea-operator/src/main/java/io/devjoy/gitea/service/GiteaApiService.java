package io.devjoy.gitea.service;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaIngressDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaRouteDependent;
import io.devjoy.gitea.k8s.dependent.gitea.GiteaServiceDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.util.ApiAccessMode;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerIngress;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
import io.fabric8.openshift.client.OpenShiftClient;
import io.quarkus.runtime.util.StringUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GiteaApiService {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaApiService.class);
	
	
	private ApiAccessMode accessMode;
	private String fallback;
	private final OpenShiftClient client;
	
	public GiteaApiService(OpenShiftClient client,
		@ConfigProperty(name = "io.devjoy.gitea.api.access.mode") ApiAccessMode accessMode, 
		@ConfigProperty(name = "io.devjoy.gitea.api.access.fallback") String fallback) 
		{
		this.client = client;
		this.accessMode = accessMode;
		this.fallback = fallback;
	}

	public void setAccessMode(ApiAccessMode accessMode) {
		this.accessMode = accessMode;
	}

	public Optional<String> getBaseUri(Gitea gitea) {
		LOG.debug("Determine {} base uri", accessMode);
		if (accessMode == ApiAccessMode.EXTERNAL && (gitea.getSpec() == null || gitea.getSpec().isIngressEnabled())) {
			if (client.supportsOpenShiftAPIGroup(OpenShiftAPIGroups.ROUTE)) {
				return getRouterBaseUri(gitea);
			} else {
				return getIngressBaseUri(gitea);
			}
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

	public Optional<String> getIngressBaseUri(Gitea gitea) {
		Optional<Ingress> route = Optional.ofNullable(GiteaIngressDependent.getResource(gitea, client).get());
		return route.map(r -> getIngressBaseUri(gitea, r));
	}

	/**
	 * Use for cluster-external testing 
	 * @param resource
	 * @param r
	 * @return
	 */
	private String getIngressBaseUri(Gitea resource, Ingress ingress) {
		String protocol = "http" + (resource.getSpec() != null && resource.getSpec().isSsl() ? "s://" : "://");
		var ing = ingress.getStatus().getLoadBalancer().getIngress();
		var hostOrIp = ing.stream()
				.filter(i -> !StringUtil.isNullOrEmpty(i.getHostname()))
				.map(IngressLoadBalancerIngress::getHostname)
				.findFirst()
				.or((() -> ing.stream()
						.filter(i -> !StringUtil.isNullOrEmpty(i.getHostname()))
						.map(IngressLoadBalancerIngress::getIp)
						.findFirst()))
				.orElseGet(() -> {
					LOG.warn("No hostname and no ip, fallback to {}}.", fallback);
					return fallback;
				});
		return protocol + hostOrIp;
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
