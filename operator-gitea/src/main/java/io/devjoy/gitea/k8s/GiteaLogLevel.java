package io.devjoy.gitea.k8s;

public enum GiteaLogLevel {
	TRACE,DEBUG,INFO,WARN,ERROR,CRITICAL,NONE;
	
	@Override
	public String toString() {
		return name().substring(0, 1).toUpperCase() + name().substring(1);
	}
}
