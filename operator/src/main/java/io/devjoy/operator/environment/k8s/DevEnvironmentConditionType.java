package io.devjoy.operator.environment.k8s;

public enum DevEnvironmentConditionType {
	UNKNOWN_ERROR("devjoy.io/UnknownError"), 
	ENV_REQUIREMENT_NOT_FOUND("devjoy.io/EnvironmentRequirementNotFound");

	private final String value;

	DevEnvironmentConditionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static DevEnvironmentConditionType fromValue(String value) {
		for (DevEnvironmentConditionType type : values())
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		throw new IllegalArgumentException();
	}
}
