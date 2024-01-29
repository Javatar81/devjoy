package io.devjoy.gitea.k8s.dependent.rhsso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;


public class KeycloakOperatorReconcileCondition implements Condition<Route, Gitea> {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakOperatorReconcileCondition.class);
    
    @Override
    public boolean isMet(DependentResource<Route, Gitea> dependentResource, Gitea primary, Context<Gitea> context) {
        boolean met = primary.getSpec() != null && primary.getSpec().isSso();
        LOG.debug("Keycloak operator reconcilation={}", met);
        return met;
    }
}