package io.devjoy.operator.project.k8s;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.knative.pkg.apis.Condition;

public class InitStatus {
    
    private List<Condition> pipelineRunConditions = new ArrayList<>();
    private String message;

    public List<Condition> getPipelineRunConditions() {
        return pipelineRunConditions;
    }

    public void setPipelineRunConditions(List<Condition> pipelineRunConditions) {
        this.pipelineRunConditions = pipelineRunConditions;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
