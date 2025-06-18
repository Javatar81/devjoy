package io.devjoy.gitea.organization.k8s.dependent;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openapi.quarkus.gitea_json.model.Organization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.k8s.dependent.gitea.GiteaAdminSecretDependent;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.gitea.organization.k8s.model.GiteaOrganization;
import io.devjoy.gitea.service.OrganizationService;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.openshift.client.OpenShiftClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.Creator;
import io.javaoperatorsdk.operator.processing.dependent.Matcher;
import io.javaoperatorsdk.operator.processing.dependent.Updater;
import io.javaoperatorsdk.operator.processing.dependent.external.PerResourcePollingDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.event.ResourceID;
import jakarta.inject.Inject;

@KubernetesDependent
public class GiteaOrganizationDependent extends PerResourcePollingDependentResource<Organization, GiteaOrganization> 
	implements Creator<Organization, GiteaOrganization>, Updater<Organization, GiteaOrganization> {
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
		LOG.info("Desired org is {}", org);
		return org;
	}

	@Override
	public Organization create(Organization desired,
			GiteaOrganization primary,
	      Context<GiteaOrganization> context) {
		//if(true) throw new IllegalStateException();
		LOG.debug("Creating org {}", primary.getMetadata().getName());
		return primary.associatedGitea(client).flatMap(g -> 
		Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
			.flatMap(GiteaAdminSecretDependent::getAdminToken)
			.flatMap(t -> 
				orgService.create(g, primary.getSpec().getOwner(),t, desired)
			)
		).orElseThrow(() -> new IllegalStateException("Gitea must be available before org can be created"));
	  }

	@Override
	public Organization update(Organization actual, Organization desired,
			GiteaOrganization primary, Context<GiteaOrganization> context) {
		return primary.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> 
					orgService.update(g, primary.getMetadata().getName(), t, desired)
				)
			).orElseThrow(() -> new IllegalStateException("Gitea must be available before org can be created"));
	}
	
	@Override
	public void delete(GiteaOrganization primary, Context<GiteaOrganization> context) {
		LOG.debug("Deleting org {}", primary.getMetadata().getName());
		primary.associatedGitea(client).ifPresent(g -> 
		Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
			.flatMap(GiteaAdminSecretDependent::getAdminToken)
			.ifPresent(t -> 
				orgService.delete(g, primary.getSpec().getOwner(), t)
			)
		);
	}

	
	@Override
	public Set<Organization> fetchResources(GiteaOrganization primaryResource) {
		LOG.error("Fetching organization resources");
		Optional<Organization> org = primaryResource.associatedGitea(client).flatMap(g -> 
			Optional.ofNullable(GiteaAdminSecretDependent.getResource(g, client).get())
				.flatMap(GiteaAdminSecretDependent::getAdminToken)
				.flatMap(t -> 
					orgService.get(g, primaryResource.getMetadata().getName(),t)
				)
		);
		LOG.error("Org found? {}", org.isPresent());
		return org.map(Set::of).orElse(Collections.emptySet());
	}

	@Override
	protected Optional<Organization> selectTargetSecondaryResource(Set<Organization> secondaryResources,
			GiteaOrganization primary, Context<GiteaOrganization> context) {
			Organization desired = desired(primary, context);
		var targetResources = secondaryResources.stream().filter(r -> orgEquals(r, desired)).toList();
		if (targetResources.size() > 1) {
			throw new IllegalStateException(
				"More than one secondary resource related to primary: " + targetResources);
		}
		return targetResources.isEmpty() ? Optional.empty() : Optional.of(targetResources.get(0));

	}
	
	private static boolean orgEquals(Organization a, Organization b) {
		return Objects.equals(a.getName(), b.getName()) &&
               Objects.equals(a.getUsername(), b.getUsername());
	}
}
