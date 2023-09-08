package io.devjoy.gitea.k8s.rhsso;

import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.Gitea;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class KeycloakOperatorGroupDependentResource extends CRUDKubernetesDependentResource<OperatorGroup, Gitea>{
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakOperatorGroupDependentResource.class);
	
	public KeycloakOperatorGroupDependentResource() {
		super(OperatorGroup.class);
	}
	

	@Override
	public OperatorGroup create(OperatorGroup target, Gitea primary, Context<Gitea> context) {
		Optional<OperatorGroup> groupWithTargetNamespace = client.resources(OperatorGroup.class).inNamespace(primary.getMetadata().getNamespace())
			.list().getItems()
			.stream().filter(g -> g.getSpec().getTargetNamespaces() != null && g.getSpec().getTargetNamespaces().contains(primary.getMetadata().getNamespace()))
			.findAny();
		return groupWithTargetNamespace.orElse(super.create(target, primary, context));
	}
	
	@Override
	protected OperatorGroup desired(Gitea primary, Context<Gitea> context) {
		LOG.debug("Creating desired operator group");
		OperatorGroup operatorGroup = client.resources(OperatorGroup.class)
				.load(getClass().getClassLoader().getResourceAsStream("manifests/rhsso/operator-group.yaml"))
				.item();
		operatorGroup.getMetadata().setNamespace(primary.getMetadata().getNamespace());
		
		if (operatorGroup.getSpec().getTargetNamespaces() == null) {
			operatorGroup.getSpec().setTargetNamespaces(new ArrayList<>());
		}
		operatorGroup.getSpec().getTargetNamespaces().add(primary.getMetadata().getNamespace());
		return operatorGroup;
	}	
	
	static Resource<OperatorGroup> getResource(KubernetesClient client, Gitea gitea) {
		return client.resources(OperatorGroup.class).inNamespace(gitea.getMetadata().getNamespace()).withName("rhsso-group");
	}

}
