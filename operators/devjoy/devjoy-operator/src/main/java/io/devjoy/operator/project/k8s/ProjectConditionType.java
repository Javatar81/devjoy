package io.devjoy.operator.project.k8s;

public enum ProjectConditionType {
	UNKNOWN_ERROR("devjoy.io/UnknownError"), 
	ENV_NOT_FOUND("devjoy.io/EnvironmentNotFound"),
	PIPELINES_API_UNAVAILABLE("devjoy.io/PipelinesApiUnavailable"),
	GITOPS_API_UNAVAILABLE("devjoy.io/GitopsApiUnavailable");

	private final String value;

	ProjectConditionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static ProjectConditionType fromValue(String value) {
		for (ProjectConditionType type : values())
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		throw new IllegalArgumentException();
	}
}
