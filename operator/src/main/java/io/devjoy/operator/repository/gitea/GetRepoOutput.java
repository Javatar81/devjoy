package io.devjoy.operator.repository.gitea;

import static io.devjoy.operator.repository.domain.GitProvider.GITEA;
import static io.devjoy.operator.repository.domain.Visibility.PRIVATE;
import static io.devjoy.operator.repository.domain.Visibility.PUBLIC;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.devjoy.operator.repository.domain.Repository;

public class GetRepoOutput {

	private String name;
	@JsonProperty("private")
	private boolean repoPrivate;
	@JsonProperty("clone_url")
	private String cloneUrl;

	public GetRepoOutput() {
		super();
	}
	
	public Repository toRepository() {
		return Repository.builder()
				.withName(name)
				.withProvider(GITEA)
				.withCloneUrl(cloneUrl)
				.withVisibility(repoPrivate ? PRIVATE : PUBLIC)
				.build();
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
	public String getCloneUrl() {
		return cloneUrl;
	}
	public void setCloneUrl(String cloneUrl) {
		this.cloneUrl = cloneUrl;
	}
}
