package io.devjoy.gitea.k8s.model;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.Condition;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(registerFullHierarchy = true)
public class GiteaStatus {

	private List<Condition> conditions = new ArrayList<>();
	private String host;
	private String version;
	private long observedGeneration;

	public long getObservedGeneration() {
		return observedGeneration;
	}
	public void setObservedGeneration(long observedGeneration) {
		this.observedGeneration = observedGeneration;
	}
	public List<Condition> getConditions() {
		return conditions;
	}
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
}
