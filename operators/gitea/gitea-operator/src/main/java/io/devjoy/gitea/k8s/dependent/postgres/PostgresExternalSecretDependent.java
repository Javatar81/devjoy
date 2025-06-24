package io.devjoy.gitea.k8s.dependent.postgres;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;

@KubernetesDependent
public class PostgresExternalSecretDependent extends KubernetesDependentResource<Secret, Gitea> {

    public PostgresExternalSecretDependent() {
        super(Secret.class);
    }

}