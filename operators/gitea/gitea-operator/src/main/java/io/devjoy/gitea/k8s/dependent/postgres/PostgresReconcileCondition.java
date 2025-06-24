package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class PostgresReconcileCondition implements Condition<HasMetadata, Gitea> {

    @Override
    public boolean isMet(DependentResource<HasMetadata, Gitea> dependentResource, Gitea primary,
            Context<Gitea> context) {
        return primary.getSpec() == null || primary.getSpec().getPostgres() == null || primary.getSpec().getPostgres().isManaged();
    }
    
}
