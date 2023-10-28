package io.devjoy.gitea.repository.k8s;

import java.util.Collections;
import java.util.List;
import java.util.Objects;


import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.annotation.Generated;

public class WebhookSpec {
	@JsonPropertyDescription("The target url of the webhook.")
	private String targetUrl;
	@JsonPropertyDescription("The http method of the webhook. Either POST or GET")
	private String httpMethod = "POST";
	@JsonPropertyDescription("The list of events to trigger the webhook, e.g. push")
	private List<String> events;
	@JsonPropertyDescription("The branch to trigger the webhook, e.g. main or * for all branches")
	private String branchFilter;
	@JsonPropertyDescription("The type of the webhook. One of dingtalk, discord, gitea, gogs, msteams, slack, telegram, feishu, wechatwork, packagist.")
	private String type;
	@JsonPropertyDescription("Wether this webhook is active. Default is true.")
	private boolean active = true;
	@JsonPropertyDescription("A secret containing the secret of the webhook. By default the user secret is referred.")
	private SecretReferenceSpec secretRef;

	public WebhookSpec() {
		super();
	}

	@Generated("SparkTools")
	private WebhookSpec(Builder builder) {
		this.targetUrl = builder.targetUrl;
		this.httpMethod = builder.httpMethod;
		this.events = builder.events;
		this.branchFilter = builder.branchFilter;
		this.type = builder.type;
		this.active = builder.active;
		this.secretRef = builder.secretRef;
	}
	
	public String getTargetUrl() {
		return targetUrl;
	}
	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}
	public String getHttpMethod() {
		return httpMethod;
	}
	public void setHttpMethod(String httpMethod) {
		this.httpMethod = httpMethod;
	}
	public List<String> getEvents() {
		return events;
	}
	public void setEvents(List<String> events) {
		this.events = events;
	}
	public String getBranchFilter() {
		return branchFilter;
	}
	public void setBranchFilter(String branchFilter) {
		this.branchFilter = branchFilter;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public boolean isActive() {
		return active;
	}
	public void setActive(boolean active) {
		this.active = active;
	}
	public SecretReferenceSpec getSecretRef() {
		return secretRef;
	}
	public void setSecretRef(SecretReferenceSpec secretRef) {
		this.secretRef = secretRef;
	}
	@Override
	public String toString() {
		return String.format(
				"WebhookSpec [targetUrl=%s, httpMethod=%s, events=%s, branchFilter=%s, type=%s, active=%s, secretRef=%s]",
				targetUrl, httpMethod, events, branchFilter, type, active, secretRef);
	}
	@Override
	public int hashCode() {
		return Objects.hash(active, branchFilter, events, httpMethod, secretRef, targetUrl, type);
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WebhookSpec other = (WebhookSpec) obj;
		return active == other.active && Objects.equals(branchFilter, other.branchFilter)
				&& Objects.equals(events, other.events) && Objects.equals(httpMethod, other.httpMethod)
				&& Objects.equals(secretRef, other.secretRef) && Objects.equals(targetUrl, other.targetUrl)
				&& type == other.type;
	}
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	@Generated("SparkTools")
	public static final class Builder {
		private String targetUrl;
		private String httpMethod = "POST";
		private List<String> events = Collections.emptyList();
		private String branchFilter;
		private String type;
		private boolean active = true;
		private SecretReferenceSpec secretRef;

		private Builder() {
		}

		public Builder withTargetUrl(String targetUrl) {
			this.targetUrl = targetUrl;
			return this;
		}

		public Builder withHttpMethod(String httpMethod) {
			this.httpMethod = httpMethod;
			return this;
		}

		public Builder withEvents(List<String> events) {
			this.events = events;
			return this;
		}

		public Builder withBranchFilter(String branchFilter) {
			this.branchFilter = branchFilter;
			return this;
		}

		public Builder withType(String type) {
			this.type = type;
			return this;
		}

		public Builder withActive(boolean active) {
			this.active = active;
			return this;
		}

		public Builder withSecretRef(SecretReferenceSpec secretRef) {
			this.secretRef = secretRef;
			return this;
		}

		public WebhookSpec build() {
			return new WebhookSpec(this);
		}
	}
	
	
	
}
