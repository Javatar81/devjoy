package io.devjoy.operator.project.k8s;

public class EnvironmentNotFoundException extends RuntimeException{
    private final String environmentName;
    private final String environmentNamespace;
    
    public EnvironmentNotFoundException(Project project) {
        this.environmentName = project.getSpec().getEnvironmentName();
        this.environmentNamespace = project.getSpec().getEnvironmentNamespace();
    }
    public EnvironmentNotFoundException(String message, Project project) {
        super(message);
        this.environmentName = project.getSpec().getEnvironmentName();
        this.environmentNamespace = project.getSpec().getEnvironmentNamespace();
    }
    public String getEnvironmentName() {
        return environmentName;
    }
    public String getEnvironmentNamespace() {
        return environmentNamespace;
    }

}
