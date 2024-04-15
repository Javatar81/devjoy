package io.devjoy.gitea.service;

import io.devjoy.gitea.k8s.model.GiteaConditionType;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganizationConditionType;

public class ServiceException extends RuntimeException {

	private static final long serialVersionUID = -2913652745734882140L;
	private final GiteaConditionType errorConditionType;
	private final GiteaOrganizationConditionType orgErrorConditionType;
	
	public ServiceException() {
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
		orgErrorConditionType = null;
	}

	public ServiceException(String message) {
		super(message);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
		orgErrorConditionType = null;
	}

	public ServiceException(Throwable cause) {
		super(cause);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
		orgErrorConditionType = null;
	}
	
	public ServiceException(String message, Throwable cause) {
		super(message, cause);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
		orgErrorConditionType = null;
	}

	public ServiceException(String message, Throwable cause, GiteaConditionType errorConditionType) {
		super(message, cause);
		this.errorConditionType = errorConditionType;
		orgErrorConditionType = null;
	}
	
	public ServiceException(String message, Throwable cause, GiteaOrganizationConditionType errorConditionType) {
		super(message, cause);
		this.errorConditionType = null;
		this.orgErrorConditionType = errorConditionType;
	}

	public ServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		this.errorConditionType = GiteaConditionType.GITEA_UNKNOWN_ERROR;
		orgErrorConditionType = null;
	}

	public GiteaConditionType getGiteaErrorConditionType() {
		return errorConditionType;
	}
	
	public GiteaOrganizationConditionType getGiteaOrgErrorConditionType() {
		return orgErrorConditionType;
	}

}
