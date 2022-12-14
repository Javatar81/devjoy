package io.devjoy.operator.repository.domain;

import javax.annotation.Generated;

public class Repository {
	private final String user;
	private final String name;
	private final String cloneUrl;
	private final Visibility visibility;
	private final GitProvider provider;

	@Generated("SparkTools")
	private Repository(Builder builder) {
		this.user = builder.user;
		this.name = builder.name;
		this.cloneUrl = builder.cloneUrl;
		this.visibility = builder.visibility;
		this.provider = builder.provider;
	}

	public String getUser() {
		return user;
	}
	public String getCloneUrl() {
		return cloneUrl;
	}
	public String getName() {
		return name;
	}
	public Visibility getVisibility() {
		return visibility;
	}
	public GitProvider getProvider() {
		return provider;
	}

	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	@Generated("SparkTools")
	public static final class Builder {
		private String user;
		private String name;
		private String cloneUrl;
		private Visibility visibility;
		private GitProvider provider;

		private Builder() {
		}

		public Builder withUser(String user) {
			this.user = user;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withCloneUrl(String cloneUrl) {
			this.cloneUrl = cloneUrl;
			return this;
		}

		public Builder withVisibility(Visibility visibility) {
			this.visibility = visibility;
			return this;
		}

		public Builder withProvider(GitProvider provider) {
			this.provider = provider;
			return this;
		}

		public Repository build() {
			return new Repository(this);
		}
	}
}
