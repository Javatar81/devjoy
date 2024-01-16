package io.devjoy.operator.environment.k8s;

public class DevEnvironmentRequirementException extends RuntimeException{
    private final String requirementName;
    private final DevEnvironment environment;
    
    public DevEnvironmentRequirementException(String requirementName, DevEnvironment environment) {
        super(String.format("Missing requirement %s for environment %s", requirementName, environment));
        this.requirementName = requirementName;
        this.environment = environment;
    }
   
    public String getRequirementName() {
        return requirementName;
    }

    public DevEnvironment getEnvironment() {
        return environment;
    }

}