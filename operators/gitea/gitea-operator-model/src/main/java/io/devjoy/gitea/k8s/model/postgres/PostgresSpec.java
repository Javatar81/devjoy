package io.devjoy.gitea.k8s.model.postgres;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class PostgresSpec {
	@JsonPropertyDescription("Whether the database instance is managed by the operator. Default is true.")
	private boolean managed = true;
	@JsonPropertyDescription("The database if the postgres database is managed by the operator, i.e. managed == true")
	private PostgresManagedConfig managedConfig = new PostgresManagedConfig();
	@JsonPropertyDescription("The database if the postgres database is not managed by the operator, i.e. managed == false")
	private PostgresUnmanagedConfig unmanagedConfig;

	public boolean isManaged() {
		return managed;
	}
	public void setManaged(boolean managed) {
		this.managed = managed;
		if (managed) {
			unmanagedConfig = null;
		} else {
			managedConfig = null;
		}
	}
	public PostgresUnmanagedConfig getUnmanagedConfig() {
		return unmanagedConfig;
	}
	public void setUnmanagedConfig(PostgresUnmanagedConfig unmanagedConfig) {
		this.unmanagedConfig = unmanagedConfig;
	}
	public PostgresManagedConfig getManagedConfig() {
		return managedConfig;
	}
	public void setManagedConfig(PostgresManagedConfig managedConfig) {
		this.managedConfig = managedConfig;
	}
}
