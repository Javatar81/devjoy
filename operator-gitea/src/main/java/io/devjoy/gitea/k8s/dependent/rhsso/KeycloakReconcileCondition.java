package io.devjoy.gitea.k8s.dependent.rhsso;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;


public class KeycloakReconcileCondition implements Condition<Route, Gitea> {
    private static final Logger LOG = LoggerFactory.getLogger(KeycloakReconcileCondition.class);
    public static String KEYCLOAK_API_VERSION = "v1";

    @Override
    public boolean isMet(DependentResource<Route, Gitea> dependentResource, Gitea primary, Context<Gitea> context) {
        boolean met = primary.getSpec() != null 
            && primary.getSpec().isSso() 
            && context.getClient().apiextensions().getApiGroup("keycloak.org") != null 
            && KEYCLOAK_API_VERSION.equals(context.getClient().apiextensions().getApiGroup("keycloak.org").getApiVersion());
        if (!met) {
            boolean apiGroupExists = context.getClient().apiextensions().getApiGroup("keycloak.org") != null;
            LOG.warn("Keycloak will not be provided. Property sso={}, apiGroupAvailable={}, apiVersion={}", primary.getSpec() != null && primary.getSpec().isSso(),
                apiGroupExists,
                apiGroupExists? context.getClient().apiextensions().getApiGroup("keycloak.org").getApiVersion() : "");
        }
        return met;
    }
}