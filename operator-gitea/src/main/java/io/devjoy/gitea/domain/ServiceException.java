package io.devjoy.gitea.domain;

import io.devjoy.gitea.k8s.GiteaConditionType;

public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = -2913652745734882140L;
	private final GiteaConditionType errorConditionType;
	
	public ServiceException() {
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
	}

	public ServiceException(String message) {
		super(message);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
	}

	public ServiceException(Throwable cause) {
		super(cause);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
	}
	
	public ServiceException(String message, Throwable cause) {
		super(message, cause);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
	}

	public ServiceException(String message, Throwable cause, GiteaConditionType errorConditionType) {
		super(message, cause);
		this.errorConditionType = errorConditionType;
	}

	public ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
	}

	public GiteaConditionType getErrorConditionType() {
		return errorConditionType;
	}

}
