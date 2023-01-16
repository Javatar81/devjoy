package io.devjoy.operator.project.k8s;

import java.util.List;

public class QuarkusSpec {
	
	private boolean enabled;
	private List<String> extensions;
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public List<String> getExtensions() {
		return extensions;
	}
	public void setExtensions(List<String> extensions) {
		this.extensions = extensions;
	}
	
	
}
