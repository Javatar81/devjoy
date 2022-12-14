package io.devjoy.operator.repository.gitea;

import static io.devjoy.operator.repository.domain.Visibility.PRIVATE;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.devjoy.operator.repository.domain.Repository;

public class CreateRepoInput {

	private String name;
	@JsonProperty("private")
	private boolean repoPrivate;
	
	public CreateRepoInput() {
		super();
	}
	
	public CreateRepoInput(Repository repository) {
		this.name = repository.getName();
		this.repoPrivate = repository.getVisibility() == PRIVATE;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isRepoPrivate() {
		return repoPrivate;
	}
	public void setRepoPrivate(boolean repoPrivate) {
		this.repoPrivate = repoPrivate;
	}
	
	

}
