package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackend;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressServiceBackendBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressTLSBuilder;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.quarkus.runtime.util.StringUtil;

@KubernetesDependent
public class GiteaIngressDependent extends CRUDKubernetesDependentResource<Ingress, Gitea> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaIngressDependent.class);
	
	public GiteaIngressDependent() {
		super(Ingress.class);
	}
	
	@Override
	protected Ingress desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Reconciling");
		Ingress ingress = context.getClient().network().v1().ingresses()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/ingress.yaml"))
				.item();
		ingress.getMetadata().setName(getName(primary));
		ingress.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		var spec = primary.getSpec();
		if (spec != null && !StringUtil.isNullOrEmpty(spec.getRoute())) {
			ingress.getSpec().getRules().get(0).setHost(spec.getRoute());
		}
		if (primary.getSpec() != null && primary.getSpec().isSsl()) {
			ingress.getSpec().setTls(List.of(new IngressTLSBuilder().withSecretName(primary.getSpec().getTlsSecret()).build()));
		}
		ingress.getSpec().getRules().get(0).getHttp().getPaths().get(0).getBackend().getService().setName(GiteaServiceDependent.getName(primary));
		
		return ingress;
	}
	
	public static Resource<Ingress> getResource(Gitea primary, OpenShiftClient client) {
		return client.network().v1().ingresses().inNamespace(primary.getMetadata().getNamespace()).withName(getName(primary));
	}

	public static String getName(Gitea primary) {
		return primary.getMetadata().getName();
	}
}