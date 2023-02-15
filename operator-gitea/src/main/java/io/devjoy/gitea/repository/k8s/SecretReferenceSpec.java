package io.devjoy.gitea.repository.k8s;

import javax.annotation.Generated;

public class SecretReferenceSpec {
	private String namespace;
	private String name;
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
