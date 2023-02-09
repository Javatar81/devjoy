package io.devjoy.gitea.k8s.rhsso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class KeycloakSubscriptionDependentResource extends CRUDKubernetesDependentResource<Subscription, Gitea>{
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakSubscriptionDependentResource.class);
	
	public KeycloakSubscriptionDependentResource() {
		super(Subscription.class);
	}
	
	@Override
	public Subscription create(Subscription target, Gitea primary, Context<Gitea> context) {
		Subscription subscription = getResource(client, primary).get();
		if (subscription != null) {
			LOG.debug("Subscription found. Skipping creation.");
			return subscription;
		} else {
			return super.create(target, primary, context);
		}
	}
	
	@Override
	protected Subscription desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired state from gitea {}", primary.getMetadata().getName());
		Subscription subscription = client.resources(Subscription.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/rhsso/subscription.yaml")).get();
		subscription.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return subscription;
	}
	
	static Resource<Subscription> getResource(KubernetesClient client, Gitea gitea) {
		return client.resources(Subscription.class).inNamespace(gitea.getMetadata().getNamespace()).withName("rhsso-operator");
	}

}
