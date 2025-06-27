package io.devjoy.gitea.k8s.dependent.gitea;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class GiteaIngressReconcileCondition implements Condition<Route, Gitea> {
    private static final Logger LOG = LoggerFactory.getLogger(GiteaIngressReconcileCondition.class);

    @Override
    public boolean isMet(DependentResource<Route, Gitea> dependentResource, Gitea primary, Context<Gitea> context) {
        boolean met = (primary.getSpec() == null || primary.getSpec().isIngressEnabled());
        LOG.debug("Route reconcilation active={}", met);
        return met;
    }
}