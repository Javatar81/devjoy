package io.devjoy.gitea.repository.k8s;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import io.fabric8.kubernetes.api.model.Condition;
import io.javaoperatorsdk.operator.api.ObservedGenerationAwareStatus;

public class GiteaRepositoryStatus extends ObservedGenerationAwareStatus {
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
	private String repositoryCreated;
    private String repositoryExists;
    private String cloneUrl;
    private String internalCloneUrl;
    private List<Condition> conditions = new ArrayList<>();

	public List<Condition> getConditions() {
		return conditions;
	}

	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
    
   
    
	public String getRepositoryCreated() {
		return repositoryCreated;
	}
	public void setRepositoryCreated(String repositoryCreated) {
		this.repositoryCreated = repositoryCreated;
	}
	public void emitRepositoryCreated() {
		this.repositoryCreated = String.format("%s", formatter.format(LocalDateTime.now()));
	}
	public String getRepositoryExists() {
		return repositoryExists;
	}
	public void setRepositoryExists(String repositoryExists) {
		this.repositoryExists = repositoryExists;
	}
	public void emitRepositoryExists() {
		this.repositoryExists = String.format("%s", formatter.format(LocalDateTime.now()));
	}
	public String getCloneUrl() {
		return cloneUrl;
	}
	public void setCloneUrl(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}
	public String getInternalCloneUrl() {
		return internalCloneUrl;
	}
	public void setInternalCloneUrl(String internalCloneUrl) {
		this.internalCloneUrl = internalCloneUrl;
	}
}
