package io.devjoy.gitea.organization.k8s.dependent;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.openapi.quarkus.gitea_json.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.service.OrganizationService;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.external.PerResourcePollingDependentResource;
import jakarta.inject.Inject;

public class GiteaOrganizationDependent extends PerResourcePollingDependentResource<Organization, GiteaOrganization> 
	implements /*DependentResourceWithExplicitState<ExternalResource, ExternalStateCustomResource, ConfigMap>,*/
	 	Creator<Organization, GiteaOrganization>, //Deleter<GiteaOrganization>,
		Updater<Organization, GiteaOrganization> {
	private static final Logger LOG = LoggerFactory.getLogger(GiteaOrganizationDependent.class);
	@Inject
	OrganizationService orgService;
	@Inject
	OpenShiftClient client;
	
	public GiteaOrganizationDependent() {
		super(Organization.class);
	}
	
	@Override
	protected Organization desired(GiteaOrganization primary, Context<GiteaOrganization> context) {
		Organization org = new Organization();
		org.setName(primary.getMetadata().getName());
		org.setDescription(primary.getSpec().getDescription());
		org.setLocation(primary.getSpec().getLocation());
		org.setUsername(primary.getMetadata().getName());
		org.setVisibility(primary.getSpec().getVisibility().toString().toUpperCase());
		org.setEmail(primary.getSpec().getOwnerEmail());
		org.setFullName(primary.getMetadata().getName());
		org.setWebsite(primary.getSpec().getWebsite());
		return org;
	}
	
	@Override
	public Organization create(Organization desired,
			GiteaOrganization primary,
	      Context<GiteaOrganization> context) {
		
		return primary.associatedGitea(client).flatMap(g -> 
		Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
			.flatMap(GiteaAdminSecretDependent::getAdminToken)
			.flatMap(t -> {
				LOG.info("Admin token available");
				return orgService.create(g, primary.getSpec().getOwner(),t, desired);
			})
		).orElseThrow(() -> new IllegalStateException("Gitea must be available before org can be created"));
	  }

	@Override
	public Organization update(Organization actual, Organization desired,
			GiteaOrganization primary, Context<GiteaOrganization> context) {
		return primary.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> {
					LOG.info("Admin token available");
					return orgService.update(g, primary.getMetadata().getName(), t, desired);
				})
			).orElseThrow(() -> new IllegalStateException("Gitea must be available before org can be created"));
	}

	@Override
	public Set<Organization> fetchResources(GiteaOrganization primaryResource) {
		LOG.debug("Fetching organization resources");
		Optional<Organization> org = primaryResource.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> {
					LOG.debug("Admin token available");
					return orgService.get(g, primaryResource.getMetadata().getName(),t);
				})
		);
		return org.map(Set::of).orElse(Collections.emptySet());
	}

}
