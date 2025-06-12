package io.devjoy.gitea.k8s.dependent.gitea;

import io.devjoy.gitea.k8s.model.Gitea;
import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependentResource;


@KubernetesDependent
public class ExtraAdminSecretDependent extends KubernetesDependentResource<Secret, Gitea> {

  public ExtraAdminSecretDependent() {
      super(Secret.class);
  }

}
