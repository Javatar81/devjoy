package io.devjoy.gitea.organization.k8s.dependent;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.openapi.quarkus.gitea_json.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.service.UserService;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.external.PerResourcePollingDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import jakarta.inject.Inject;

@KubernetesDependent()
public class GiteaOrganizationOwnerDependent extends PerResourcePollingDependentResource<User, GiteaOrganization> 
	implements Creator<User, GiteaOrganization> {
	
	private static final Logger LOG = LoggerFactory.getLogger(GiteaOrganizationOwnerDependent.class);
	@Inject
	UserService userService;
	@Inject
	OpenShiftClient client;
	
	public GiteaOrganizationOwnerDependent() {
		super(User.class);
	}

	@Override
	protected User desired(GiteaOrganization primary, Context<GiteaOrganization> context) {
		User user = new User();
		user.setActive(true);
		user.setEmail(primary.getSpec().getOwnerEmail());
		user.setFullName(primary.getSpec().getOwner());
		user.setLoginName(primary.getSpec().getOwner());
		user.setLogin(primary.getSpec().getOwner());
		return user;
	}
	
	
	@Override
	public Set<User> fetchResources(GiteaOrganization primaryResource) {
		LOG.debug("Fetching user resources");
		Optional<User> user = primaryResource.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> 
					userService.getUser(g, primaryResource.getSpec().getOwner(), t)
				)
		);
		return user.map(Set::of).orElse(Collections.emptySet());
		
	}

	@Override
	public User create(User desired, GiteaOrganization primary, Context<GiteaOrganization> context) {

		return primary.associatedGitea(client)
				.flatMap(g -> Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
						.flatMap(GiteaAdminSecretDependent::getAdminToken)
						.flatMap(t -> userService.getUser(g, desired.getLogin(), t)
								.or(() -> userService.createUser(g, desired, t))))
				.orElseThrow(
						() -> new IllegalStateException("Gitea must be available before org owner can be created"));
	}

}
