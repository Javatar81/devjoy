package io.devjoy.gitea.k8s;

import java.util.ArrayList;

import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.k8s.model.GiteaStatus;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class GiteaStatusUpdater {
	
	
	public void init(Gitea resource) {
		if (resource.getStatus() == null) {
			GiteaStatus status = new GiteaStatus();
			status.setConditions(new ArrayList<>());
			resource.setStatus(status);
		}
	}
}
