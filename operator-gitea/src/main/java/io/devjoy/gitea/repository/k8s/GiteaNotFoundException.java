package io.devjoy.gitea.repository.k8s;


public class GiteaNotFoundException extends RuntimeException {

	private static final long serialVersionUID = -8205693419808373306L;
	private final String namespace;
    private final String name;
   
    public GiteaNotFoundException(String message, String namespace, String name) {
        super(message);
        this.namespace = namespace;
        this.name = name;
    }

    public GiteaNotFoundException(String message, String namespace) {
        super(message);
        this.namespace = namespace;
        this.name = null;
    }

    public String getNamespace() {
        return this.namespace;
    }

    public String getName() {
        return this.name;
    }
}