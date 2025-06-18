package io.devjoy.operator.project.k8s;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.Condition;

public class ProjectStatus {

    private WorkspaceStatus workspace = new WorkspaceStatus();
    private RepositoryStatus repository = new RepositoryStatus();
	private InitStatus initStatus = new InitStatus();
	private DeployStatus deployStatus = new DeployStatus();
	@JsonPropertyDescription("The conditions representing the repository status.")
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
	public WorkspaceStatus getWorkspace() {
		return workspace;
	}
	public void setWorkspace(WorkspaceStatus workspace) {
		this.workspace = workspace;
	}
	public RepositoryStatus getRepository() {
		return repository;
	}
	public void setRepository(RepositoryStatus repository) {
		this.repository = repository;
	}
	public InitStatus getInitStatus() {
		return initStatus;
	}
	public void setInitStatus(InitStatus initStatus) {
		this.initStatus = initStatus;
	}
	public DeployStatus getDeployStatus() {
		return this.deployStatus;
	}
	public void setDeployStatus(DeployStatus deployStatus) {
		this.deployStatus = deployStatus;
	}
	
}
