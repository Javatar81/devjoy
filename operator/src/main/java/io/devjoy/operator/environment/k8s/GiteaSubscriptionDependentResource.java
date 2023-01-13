package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class GiteaSubscriptionDependentResource extends CRUDKubernetesDependentResource<Subscription, DevEnvironment>{

	private static final String SUBSCRIPTION_NAME = "gitea-operator";
	private static final String SUBSCRIPTION_NS = "openshift-operators";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaSubscriptionDependentResource.class);
	
	public GiteaSubscriptionDependentResource() {
		super(Subscription.class);
	}
	
	@Override
	public Subscription create(Subscription target, DevEnvironment primary, Context<DevEnvironment> context) {
		if(enabledAndManaged(primary)) {
			return super.create(target, primary, context);
		} else {
			return getResource(client);
		}
	}

	private boolean enabledAndManaged(DevEnvironment primary) {
		return primary.getSpec() == null || primary.getSpec().getGitea() == null
				|| (primary.getSpec().getGitea().isEnabled() && primary.getSpec().getGitea().isManaged());
	}
	
	@Override
	protected Subscription desired(DevEnvironment primary, Context<DevEnvironment> context) {
		if(enabledAndManaged(primary)) {
			LOG.info("Setting desired state from DevEnvironment {}", primary.getMetadata().getName());
			return client.resources(Subscription.class)
					.load(getClass().getClassLoader().getResourceAsStream("dev/gitea-subscription.yaml"))
					.get();
		} else {
			LOG.info("Gitea subscription is not managed for {}", primary.getMetadata().getName());
			Subscription subscription = getResource(client);
			if (subscription == null) {
				LOG.warn(
						"Gitea subscription not found. Set gitea to managed state or install it in namespace {} with name {}.",
						SUBSCRIPTION_NS, SUBSCRIPTION_NAME);
			}
			return subscription;
		}
	}
	
	static Subscription getResource(KubernetesClient client) {
		return client.resources(Subscription.class).inNamespace(SUBSCRIPTION_NS).withName(SUBSCRIPTION_NAME).get();
	}

}
