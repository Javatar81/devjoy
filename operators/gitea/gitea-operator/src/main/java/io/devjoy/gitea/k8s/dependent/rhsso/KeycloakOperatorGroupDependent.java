package io.devjoy.gitea.k8s.dependent.rhsso;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent
public class KeycloakOperatorGroupDependent extends CRUDKubernetesDependentResource<OperatorGroup, Gitea>{
	private static final Logger LOG = LoggerFactory.getLogger(KeycloakOperatorGroupDependent.class);
	
	public KeycloakOperatorGroupDependent() {
		super(OperatorGroup.class);
	}
	
	@Override
	protected OperatorGroup desired(Gitea primary, Context<Gitea> context) {
		LOG.info("Creating desired operator group");
		OperatorGroup operatorGroup = context.getClient().resources(OperatorGroup.class)
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
