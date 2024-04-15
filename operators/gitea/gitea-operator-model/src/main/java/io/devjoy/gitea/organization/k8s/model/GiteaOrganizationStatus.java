package io.devjoy.gitea.organization.k8s.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.Condition;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(registerFullHierarchy = true)
public class GiteaOrganizationStatus extends ObservedGenerationAwareStatus {
	@JsonPropertyDescription("The conditions representing the organization status.")
    private List<Condition> conditions = new ArrayList<>();

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
}
