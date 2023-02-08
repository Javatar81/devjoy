package io.devjoy.gitea.k8s;

public enum GiteaConditionType {
	GITEA_UNKNOWN_ERROR("devjoy.io/UnknownError"), GITEA_ADMIN_CREATED("devjoy.io/GiteaAdminCreated"),
	GITEA_ADMIN_PW_GENERATED("devjoy.io/GiteaAdminPasswordGenerated"),
	GITEA_ADMIN_PW_IN_SECRET("devjoy.io/GiteaAdminPasswordInSecret"),
	GITEA_AUTH_SRC_CREATED("devjoy.io/GiteaAuthSourceCreated");

	private final String value;

	GiteaConditionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static GiteaConditionType fromValue(String value) {
		for (GiteaConditionType type : values())
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		throw new IllegalArgumentException();
	}
}
