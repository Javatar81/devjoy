package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;


public class GiteaOAuthClientReconcileCondition implements Condition<Route, Gitea> {
    
    @Override
    public boolean isMet(DependentResource<Route, Gitea> dependentResource, Gitea primary, Context<Gitea> context) {
        return primary.getSpec() != null && primary.getSpec().isSso();
    }
}