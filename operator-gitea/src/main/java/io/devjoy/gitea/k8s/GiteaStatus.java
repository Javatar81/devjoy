package io.devjoy.gitea.k8s;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.Condition;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class GiteaStatus extends ObservedGenerationAwareStatus {

	private List<Condition> conditions = new ArrayList<>();

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

}
