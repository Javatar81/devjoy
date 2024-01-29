package io.devjoy.gitea.k8s.dependent.gitea;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class GiteaServiceAccountDependentResource extends CRUDKubernetesDependentResource<ServiceAccount, Gitea> implements Matcher<ServiceAccount, Gitea>{
	private static final Logger LOG = LoggerFactory.getLogger(GiteaServiceAccountDependentResource.class);
	
	public GiteaServiceAccountDependentResource() {
		super(ServiceAccount.class);
	}
	
	@Override
	protected ServiceAccount desired(Gitea primary, Context<Gitea> context) {
		return getDesired(primary, context);
	}

	public ServiceAccount getDesired(Gitea primary, Context<Gitea> context) {
		LOG.info("Setting desired Gitea service account");
		ServiceAccount sa = context.getClient().serviceAccounts()
				.load(getClass().getClassLoader().getResourceAsStream("manifests/gitea/service-account.yaml")).item();
		String name = primary.getMetadata().getName();
		sa.getMetadata().setName(name);
		sa.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		return sa;
	}
	
	@Override
	public Result<ServiceAccount> match(ServiceAccount actualResource, Gitea primary,
		      Context<Gitea> context) {
		var desired = this.desired(primary, context);
		boolean equal = Objects.equals(actualResource.getMetadata().getName(), desired.getMetadata().getName())
		 && Objects.equals(actualResource.getMetadata().getNamespace(), desired.getMetadata().getNamespace());
	    return Result.computed(equal, desired);
	}
	
	public static Resource<ServiceAccount> getResource(Gitea primary, KubernetesClient client) {
		return client.serviceAccounts().inNamespace(primary.getMetadata().getNamespace()).withName(primary.getMetadata().getName());
	}

	
}
