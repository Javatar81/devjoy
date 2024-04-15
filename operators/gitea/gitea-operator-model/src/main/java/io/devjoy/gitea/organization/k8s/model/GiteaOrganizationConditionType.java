package io.devjoy.gitea.organization.k8s.model;

public enum GiteaOrganizationConditionType {
	GITEA_ORG_UNKNOWN_ERROR("devjoy.io/UnknownError"), GITEA_ORG_CREATED("devjoy.io/GiteaOrgCreated")
	, GITEA_API_ERROR("devjoy.io/GiteaApiError");
	
	private final String value;

	GiteaOrganizationConditionType(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}

	@Override
	public String toString() {
		return value;
	}

	public static GiteaOrganizationConditionType fromValue(String value) {
		for (GiteaOrganizationConditionType type : values())
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		throw new IllegalArgumentException();
	}
}
