package io.devjoy.operator.project.k8s.deploy;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import org.openapi.quarkus.application_yaml.model.ApplicationSpec;
import org.openapi.quarkus.application_yaml.model.ApplicationStatus;

@Version("v1alpha1")
@Group("argoproj.io")
public class Application extends CustomResource<ApplicationSpec, ApplicationStatus> implements Namespaced {
	
    private Object operation;

    public Object getOperation() {
        return operation;
    }

    public void setOperation(Object operation) {
        this.operation = operation;
    }
    
}
