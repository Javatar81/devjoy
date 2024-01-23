package io.devjoy.gitea.repository.k8s;

public enum GiteaRepositoryConditionType {
	GITEA_REPO_UNKNOWN_ERROR("devjoy.io/UnknownError"), 
	GITEA_NOT_FOUND("devjoy.io/GiteaNotFound"),
	GITEA_REPO_CREATED("devjoy.io/GiteaRepoCreated")
	
	;

	private final String value;

	GiteaRepositoryConditionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static GiteaRepositoryConditionType fromValue(String value) {
		for (GiteaRepositoryConditionType type : values())
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		throw new IllegalArgumentException();
	}
}
