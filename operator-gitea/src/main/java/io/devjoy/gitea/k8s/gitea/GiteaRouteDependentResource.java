package io.devjoy.gitea.k8s.gitea;

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

@KubernetesDependent
public class GiteaRouteDependentResource extends CRUDKubernetesDependentResource<Route, Gitea> {
	@Inject
	OpenShiftClient ocpClient;
	
	public GiteaRouteDependentResource() {
		super(Route.class);
	}
	
	@Override
	protected Route desired(Gitea primary, Context<Gitea> context) {
		Route route = ocpClient.routes()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/route.yaml"))
				.item();
		String name = primary.getMetadata().getName();
		route.getMetadata().setName(name);
		route.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		if (StringUtil.isNullOrEmpty(primary.getSpec().getRoute())) {
			route.getSpec().setHost(primary.getSpec().getRoute());
		}
		if (primary.getSpec().isSsl()) {
			route.getSpec().setTls(new TLSConfigBuilder().withInsecureEdgeTerminationPolicy("Redirect")
					.withTermination("edge").build());
		}
		route.getSpec().setTo(new RouteTargetReferenceBuilder().withName(name).build());
		return route;
	}
	
	public static Resource<Route> getResource(Gitea primary, OpenShiftClient client) {
		return client.routes().inNamespace(primary.getMetadata().getNamespace()).withName(primary.getMetadata().getName());
	}

}
