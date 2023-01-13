package io.devjoy.operator.environment.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.runtime.util.StringUtil;

public class SecretTokenNotChangedCondition implements Condition<Secret, DevEnvironment> {

	private static final Logger LOG = LoggerFactory.getLogger(SecretTokenNotChangedCondition.class);
	
	@Override
	public boolean isMet(DevEnvironment primary, Secret secondary, Context<DevEnvironment> context) {
		logIfDebug(secondary);
		return secondary == null || StringUtil.isNullOrEmpty(secondary.getData().get("token"));
	}

	private void logIfDebug(Secret secondary) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Condition secondary is {} ", secondary);
			if (secondary != null) {
				LOG.debug("Token is empty? {} ", StringUtil.isNullOrEmpty(secondary.getData().get("token")));
			}
		}
	}
}
