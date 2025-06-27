package io.devjoy.gitea.k8s.model;

import java.util.ArrayList;
import java.util.List;

import io.devjoy.gitea.k8s.model.keycloak.KeycloakStatus;
import io.devjoy.gitea.k8s.model.postgres.PostgresStatus;
import io.fabric8.kubernetes.api.model.Condition;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection(registerFullHierarchy = true)
public class GiteaStatus {

	private List<Condition> conditions = new ArrayList<>();
	private String host;
	private String version;
	private Integer deploymentReadyReplicas;
    private String pvcPhase;
	private KeycloakStatus keycloak;
	private PostgresStatus postgres;
	private long observedGeneration;

	public List<Condition> getConditions() {
		return conditions;
	}
	public void setConditions(List<Condition> conditions) {
		this.conditions = conditions;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public KeycloakStatus getKeycloak() {
		return keycloak;
	}
	public void setKeycloak(KeycloakStatus keycloak) {
		this.keycloak = keycloak;
	}
	public PostgresStatus getPostgres() {
		return postgres;
	}
	public void setPostgres(PostgresStatus postgres) {
		this.postgres = postgres;
	}
	public long getObservedGeneration() {
		return observedGeneration;
	}
	public void setObservedGeneration(long observedGeneration) {
		this.observedGeneration = observedGeneration;
	}
	public Integer getDeploymentReadyReplicas() {
		return deploymentReadyReplicas;
	}
	public void setDeploymentReadyReplicas(Integer deploymentReadyReplicas) {
		this.deploymentReadyReplicas = deploymentReadyReplicas;
	}
	public String getPvcPhase() {
		return pvcPhase;
	}
	public void setPvcPhase(String pvcPhase) {
		this.pvcPhase = pvcPhase;
	}
	


}
