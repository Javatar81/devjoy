package io.devjoy.gitea.repository.k8s.model;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("devjoy.io")
public class GiteaRepository extends CustomResource<GiteaRepositorySpec, GiteaRepositoryStatus> implements Namespaced {

	private static final long serialVersionUID = -8205693419808373306L;
	
@Override
public void setSpec(GiteaRepositorySpec spec) {
	// TODO Auto-generated method stub
	super.setSpec(spec);
}

}

