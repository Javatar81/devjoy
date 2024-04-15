package io.devjoy.gitea.organization.k8s.dependent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openapi.quarkus.gitea_json.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.service.UserService;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.Matcher.Result;
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
		
		//user.setUsername(primary.getSpec().getOwner());
		//user.setMustChangePassword(true);
		//user.setPassword("devjoypw");
		return user;
	}
	
	
	@Override
	public Set<User> fetchResources(GiteaOrganization primaryResource) {
		LOG.info("Fetching user resources");
		Optional<User> user = primaryResource.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> {
					LOG.info("Admin token available");
					return userService.getUser(g, primaryResource.getSpec().getOwner(), t);
				})
		);
		return user.map(Set::of).orElse(Collections.emptySet());
		
	}

	@Override
	public User create(User desired, GiteaOrganization primary, Context<GiteaOrganization> context) {
		return primary.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> 
					userService.createUser(g, desired, t)
				)
		).orElseThrow(() -> new IllegalStateException("Gitea must be available before org owner can be created"));
	}
	
	  @Override
	  public Matcher.Result<User> match(User resource,
			  GiteaOrganization primary,
	      Context<GiteaOrganization> context) {
	    return Matcher.Result.nonComputed(resource.getLogin().equals(primary.getSpec().getOwner()));
	  }

}
