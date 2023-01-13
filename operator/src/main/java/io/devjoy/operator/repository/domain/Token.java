package io.devjoy.operator.repository.domain;

import javax.annotation.Generated;

public class Token {
	private final String name;
	private final String value;

	@Generated("SparkTools")
	private Token(Builder builder) {
		this.name = builder.name;
		this.value = builder.value;
	}
	@Generated("SparkTools")
	public static Builder builder() {
		return new Builder();
	}
	@Generated("SparkTools")
	public static final class Builder {
		private String name;
		private String value;

		private Builder() {
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

		public Builder withValue(String value) {
			this.value = value;
			return this;
		}

		public Token build() {
			return new Token(this);
		}
	}
	public String getName() {
		return name;
	}
	public String getValue() {
		return value;
	}
}
