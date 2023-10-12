package io.devjoy.operator.environment.k8s.domain;

public class Environment {
	private final String name;

	private Environment(Builder builder) {
		this.name = builder.name;
	}

	public static Builder builder() {
		return new Builder();
	}

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
