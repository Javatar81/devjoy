package io.devjoy.operator.project.k8s;

import java.util.ArrayList;
import java.util.List;

import io.fabric8.knative.internal.pkg.apis.Condition;

public class InitStatus {
    private List<Condition> pipelineRunConditions = new ArrayList<>();

    public List<Condition> getPipelineRunConditions() {
        return pipelineRunConditions;
    }

    public void setPipelineRunConditions(List<Condition> pipelineRunConditions) {
        this.pipelineRunConditions = pipelineRunConditions;
    }

}
