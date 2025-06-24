package io.devjoy.gitea.k8s.model.postgres;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public class PostgresUnmanagedConfig {
    @JsonPropertyDescription("The secret containing the password.")
	private String extraSecretName;
    @JsonPropertyDescription("The host name of the database.")
    private String hostName;
    @JsonPropertyDescription("The user name to connect with the database.")
    private String userName;
    @JsonPropertyDescription("The data base name to connect with.")
    private String databaseName;

    public String getHostName() {
        return hostName;
    }
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }
    public String getUserName() {
        return userName;
    }
    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getDatabaseName() {
        return databaseName;
    }
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
    public String getExtraSecretName() {
		return extraSecretName;
	}
	public void setExtraSecretName(String extraSecretName) {
		this.extraSecretName = extraSecretName;
	}
}
