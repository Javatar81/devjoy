package io.devjoy.operator.environment.k8s;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.devjoy.operator.environment.k8s.status.ArgoCdStatus;
import io.devjoy.operator.environment.k8s.status.DevSpacesStatus;
import io.devjoy.operator.environment.k8s.status.GiteaStatus;
import io.fabric8.kubernetes.api.model.Condition;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class DevEnvironmentStatus extends ObservedGenerationAwareStatus{

    private GiteaStatus gitea;
    private DevSpacesStatus devSpaces;
	private ArgoCdStatus argoCd;
	
	@JsonPropertyDescription("The conditions representing the repository status.")
    private List<Condition> conditions = new ArrayList<>();
    
	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}

	public DevSpacesStatus getDevSpaces() {
		return devSpaces;
	}
	public void setDevSpaces(DevSpacesStatus devSpaces) {
		this.devSpaces = devSpaces;
	}
	public ArgoCdStatus getArgoCd() {
		return argoCd;
	}

	public void setArgoCd(ArgoCdStatus argoCd) {
		this.argoCd = argoCd;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((gitea == null) ? 0 : gitea.hashCode());
		result = prime * result + ((devSpaces == null) ? 0 : devSpaces.hashCode());
		result = prime * result + ((argoCd == null) ? 0 : argoCd.hashCode());
		result = prime * result + ((conditions == null) ? 0 : conditions.hashCode());
		return result;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DevEnvironmentStatus other = (DevEnvironmentStatus) obj;
		if (gitea == null) {
			if (other.gitea != null)
				return false;
		} else if (!gitea.equals(other.gitea))
			return false;
		if (devSpaces == null) {
			if (other.devSpaces != null)
				return false;
		} else if (!devSpaces.equals(other.devSpaces))
			return false;
		if (argoCd == null) {
			if (other.argoCd != null)
				return false;
		} else if (!argoCd.equals(other.argoCd))
			return false;
		if (conditions == null) {
			if (other.conditions != null)
				return false;
		} else if (!conditions.equals(other.conditions))
			return false;
		return true;
	}
	@Override
	public String toString() {
		return "DevEnvironmentStatus [giteaStatus=" + gitea + ", devSpaces=" + devSpaces + ", argoCd=" + argoCd
				+ ", conditions=" + conditions + "]";
	}

	public GiteaStatus getGitea() {
		return gitea;
	}

	public void setGitea(GiteaStatus gitea) {
		this.gitea = gitea;
	}

}
