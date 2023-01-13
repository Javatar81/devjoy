package io.devjoy.operator.project.k8s;

public class ProjectStatus {

    private WorkspaceStatus workspace;
    private RepositoryStatus repository;

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
	
}
