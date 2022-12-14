package io.devjoy.operator.repository.k8s;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class RepositoryStatus {

    private String repositoryCreated;
    private String repositoryExists;
    private String cloneUrl;
    
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    
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
}
