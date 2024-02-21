package io.devjoy.gitea.domain;

import io.quarkus.runtime.util.StringUtil;

public class Option {
	private final String name;
	private final String value;
	
	public Option(String name) {
		super();
		this.name = name;
		this.value = "";
	}
	
	public Option(String name, String value) {
		super();
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		if(!StringUtil.isNullOrEmpty(value)) {
			return String.format("--%s %s", name, value);
		} else {
			return String.format("--%s", name);
		}
	}
	
	public String[] toArray() {
		if(!StringUtil.isNullOrEmpty(value)) {
			return new String[] {String.format("--%s", name), value};
		} else {
			return new String[] {String.format("--%s", name)};
		}
		
	}
	
}
