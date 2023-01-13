package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.CatalogSource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class GiteaCatalogSourceDependentResource extends CRUDKubernetesDependentResource<CatalogSource, DevEnvironment>{
	private static final String CATALOG_SOURCE_NS = "openshift-marketplace";
	private static final String CATALOG_SOURCE_NAME = "redhat-gpte-gitea";
	private static final Logger LOG = LoggerFactory.getLogger(GiteaCatalogSourceDependentResource.class);
	
	public GiteaCatalogSourceDependentResource() {
		super(CatalogSource.class);
	}
	
	@Override
	public CatalogSource create(CatalogSource target, DevEnvironment primary, Context<DevEnvironment> context) {
		if(enabledAndManaged(primary)) {
			return super.create(target, primary, context);
		} else {
			return getResource(client).get();
		}
	}

	@Override
	protected CatalogSource desired(DevEnvironment primary, Context<DevEnvironment> context) {
		if(enabledAndManaged(primary)) {
			LOG.info("Setting desired state from DevEnvironment {}", primary.getMetadata().getName());
			return client.resources(CatalogSource.class)
					.load(getClass().getClassLoader().getResourceAsStream("dev/gitea-catalogsource.yaml"))
					.get();
		} else {
			LOG.info("Gitea catalog is not managed for {}", primary.getMetadata().getName());
			CatalogSource catalogSource = getResource(client).get();
			if (catalogSource == null) {
				LOG.warn(
						"Gitea catalog source not found. Set gitea to managed state or install it in namespace {} with name {}.",
						CATALOG_SOURCE_NS, CATALOG_SOURCE_NAME);
			}
			return catalogSource;
		}
	}

	static Resource<CatalogSource> getResource(KubernetesClient client) {
		return client.resources(CatalogSource.class).inNamespace(CATALOG_SOURCE_NS).withName(CATALOG_SOURCE_NAME);
	}
	
	private boolean enabledAndManaged(DevEnvironment primary) {
		return primary.getSpec() == null || primary.getSpec().getGitea() == null
				|| (primary.getSpec().getGitea().isEnabled() && primary.getSpec().getGitea().isManaged());
	}
}
