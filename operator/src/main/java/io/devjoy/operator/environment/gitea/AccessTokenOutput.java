package io.devjoy.operator.environment.gitea;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.devjoy.operator.repository.domain.Token;

public class AccessTokenOutput {
	private int id;
	private String name;
	@JsonProperty("sha1")
	private String sha1;
	@JsonProperty("token_last_eight")
	private String tokenLastEight;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getSha1() {
		return sha1;
	}
	public void setSha1(String sha1) {
		this.sha1 = sha1;
	}
	public String getTokenLastEight() {
		return tokenLastEight;
	}
	public void setTokenLastEight(String tokenLastEight) {
		this.tokenLastEight = tokenLastEight;
	}
	public Token toToken() {
		return Token.builder().withName(name).withValue(sha1).build();
	}
}
