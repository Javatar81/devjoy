package io.devjoy.gitea.k8s.gitea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteTargetReferenceBuilder;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;
import jakarta.inject.Inject;

@KubernetesDependent(resourceDiscriminator = GiteaRouteDiscriminator.class)
public class GiteaRouteDependentResource extends CRUDKubernetesDependentResource<Route, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaRouteDependentResource.class);
	@Inject
	OpenShiftClient ocpClient;
	
	public GiteaRouteDependentResource() {
		super(Route.class);
	}
	
	@Override
	protected Route desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Reconciling");
		Route route = ocpClient.routes()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/route.yaml"))
				.item();
		route.getMetadata().setName(getName(primary));
		route.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (!StringUtil.isNullOrEmpty(primary.getSpec().getRoute())) {
			route.getSpec().setHost(primary.getSpec().getRoute());
		}
		if (primary.getSpec().isSsl()) {
			route.getSpec().setTls(new TLSConfigBuilder().withInsecureEdgeTerminationPolicy("Redirect")
					.withTermination("edge").build());
		}
		route.getSpec().setTo(new RouteTargetReferenceBuilder().withName(getName(primary)).build());
		return route;
	}
	
	public static Resource<Route> getResource(Gitea primary, OpenShiftClient client) {
		return client.routes().inNamespace(primary.getMetadata().getNamespace()).withName(getName(primary));
	}

	public static String getName(Gitea primary) {
		return primary.getMetadata().getName();
	}

}
