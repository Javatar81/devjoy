package io.devjoy.operator.repository.k8s;

import io.devjoy.operator.repository.domain.Repository.Builder;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;



@Version("v1alpha1")
@Group("devjoy.io")
public class Repository extends CustomResource<RepositorySpec, RepositoryStatus> implements Namespaced {
	
	public io.devjoy.operator.repository.domain.Repository toRepository() {
		Builder builder = io.devjoy.operator.repository.domain.Repository.builder()
		.withName(getMetadata().getName());
		if (getSpec().getManaged() != null) {
			builder = builder.withVisibility(getSpec().getManaged().getVisibility())
			.withProvider(getSpec().getManaged().getProvider())
			.withUser(getSpec().getManaged().getUser());
		}
		return builder.build();
	}
	
	/*
	 * We need to override otherwise there is a problem calling this for superclass via DependentResource
	 */
	@Override
	public void setSpec(RepositorySpec spec) {
		// TODO Auto-generated method stub
		super.setSpec(spec);
	}
}

