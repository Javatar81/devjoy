package io.devjoy.operator.environment.k8s.deploy;

import java.util.HashMap;

import io.argoproj.v1beta1.ArgoCD;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = ArgoCDDependentResource.LABEL_TYPE_SELECTOR)
public class ArgoCDDependentResource extends CRUDKubernetesDependentResource<ArgoCD, DevEnvironment>{
    public static final String LABEL_KEY = "devjoy.io/argo.type";
	public static final String LABEL_VALUE = "deploy-argo";
	static final String LABEL_TYPE_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
    
    public ArgoCDDependentResource() {
        super(ArgoCD.class);
    }

    @Override
    protected ArgoCD desired(DevEnvironment primary, Context<DevEnvironment> context) {
       ArgoCD argo = context.getClient().resources(ArgoCD.class)
				.load(getClass().getClassLoader().getResourceAsStream("deploy/argocd.yaml"))
				.item();
       argo.getMetadata().setNamespace(primary.getMetadata().getNamespace());
       argo.getMetadata().setName(getName(primary));
       HashMap<String, String> labels = new HashMap<>();
	   labels.put(LABEL_KEY, LABEL_VALUE);
	   argo.getMetadata().setLabels(labels);
       return argo;
    }

    public static String getName(DevEnvironment primary) {
        return "argocd-" + primary.getMetadata().getName();
    }

    public static Resource<ArgoCD> getResource(DevEnvironment primary, KubernetesClient client) {
      return client.resources(ArgoCD.class).inNamespace(primary.getMetadata().getNamespace()).withName(getName(primary));
   }
}
