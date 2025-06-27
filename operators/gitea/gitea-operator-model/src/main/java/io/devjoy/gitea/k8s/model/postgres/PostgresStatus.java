package io.devjoy.gitea.k8s.model.postgres;

public class PostgresStatus {
    private Integer deploymentReadyReplicas;
    private String pvcPhase;

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
