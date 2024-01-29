package io.devjoy.operator.environment.k8s;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.quarkus.runtime.util.StringUtil;

public class SecretTokenNotChangedCondition implements Condition<Secret, DevEnvironment> {

	private static final Logger LOG = LoggerFactory.getLogger(SecretTokenNotChangedCondition.class);

	private void logIfDebug(Secret secondary) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Condition secondary is {} ", secondary);
			if (secondary != null) {
				LOG.debug("Token is empty? {} ", StringUtil.isNullOrEmpty(secondary.getData().get("token")));
			}
		}
	}

	@Override
	public boolean isMet(DependentResource<Secret, DevEnvironment> dependentResource, DevEnvironment primary,
			Context<DevEnvironment> context) {
		Optional<Secret> secondaryResource = dependentResource.getSecondaryResource(primary, context);
		secondaryResource.ifPresent(s -> logIfDebug(s));
		return secondaryResource.map(s -> StringUtil.isNullOrEmpty(s.getData().get("token"))).orElse(true);
	}
}
