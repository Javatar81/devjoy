package io.devjoy.gitea.k8s;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.openshift.api.model.Project;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class OpenShiftActivationCondition implements Condition<Object, HasMetadata> {

    @Override
    public boolean isMet(DependentResource<Object, HasMetadata> dependentResource, HasMetadata primary,
            Context<HasMetadata> context) {
       return serverSupportsApi(context.getClient());         
    }
    public static boolean serverSupportsApi(KubernetesClient client) {
      return client.supports(Project.class);
    }

}