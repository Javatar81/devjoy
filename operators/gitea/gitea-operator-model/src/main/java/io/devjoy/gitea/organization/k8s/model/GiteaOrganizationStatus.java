package io.devjoy.gitea.organization.k8s.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.Condition;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(registerFullHierarchy = true)
public class GiteaOrganizationStatus {
	@JsonPropertyDescription("The conditions representing the organization status.")
    private List<Condition> conditions = new ArrayList<>();

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
}
