package io.devjoy.operator.environment.k8s.domain;

import javax.annotation.Generated;

public class Environment {
	private final String name;

	@Generated("SparkTools")
	private Environment(Builder builder) {
		this.name = builder.name;
	}

	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}

	@Generated("SparkTools")
	public static final class Builder {
		private String name;

		private Builder() {
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Environment build() {
			return new Environment(this);
		}
	}

	public String getName() {
		return name;
	}
	
}
