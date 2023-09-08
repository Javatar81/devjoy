package io.devjoy.gitea.k8s.postgres;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PostgresConfig {
	
	public String getDatabaseName() {
		return "giteadb";
	}
	
	public String getUserName() {
		return "giteauser";
	}
	
	public String getPassword() {
		return "giteapassword";
	}
}
