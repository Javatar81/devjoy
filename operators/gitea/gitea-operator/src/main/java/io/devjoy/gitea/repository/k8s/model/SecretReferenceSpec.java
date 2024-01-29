package io.devjoy.gitea.repository.k8s.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import jakarta.annotation.Generated;

public class SecretReferenceSpec {
	@JsonPropertyDescription("The namespace of the referenced secret. If empty, it points to the same namespace as the repository resource.")
	private String namespace;
	@JsonPropertyDescription("The name of the referenced secret.")
	@JsonProperty(required = true)
	private String name;
	@JsonPropertyDescription("The key storing the password as value.")
	private String key;

	public SecretReferenceSpec() {
		super();
	}

	@Generated("SparkTools")
	private SecretReferenceSpec(Builder builder) {
		this.namespace = builder.namespace;
		this.name = builder.name;
		this.key = builder.key;
	}
	
	public String getNamespace() {
		return namespace;
	}
	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	@Generated("SparkTools")
	public static final class Builder {
		private String namespace;
		private String name;
		private String key;

		private Builder() {
		}

		public Builder withNamespace(String namespace) {
			this.namespace = namespace;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withKey(String key) {
			this.key = key;
			return this;
		}

		public SecretReferenceSpec build() {
			return new SecretReferenceSpec(this);
		}
	}
	@Override
	public String toString() {
		return String.format("SecretReferenceSpec [namespace=%s, name=%s, key=%s]", namespace, name, key);
	}
}
