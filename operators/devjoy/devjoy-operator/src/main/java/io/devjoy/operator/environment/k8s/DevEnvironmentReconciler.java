package io.devjoy.operator.environment.k8s;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.argoproj.v1beta1.ArgoCD;
import io.devjoy.gitea.k8s.model.Gitea;
import io.devjoy.operator.environment.k8s.build.BuildEventListenerDependent;
import io.devjoy.operator.environment.k8s.build.BuildPipelineDependent;
import io.devjoy.operator.environment.k8s.build.BuildPushTriggerTemplateDependent;
import io.devjoy.operator.environment.k8s.build.EventListenerActivationCondition;
import io.devjoy.operator.environment.k8s.build.GiteaPushTriggerBindingDependent;
import io.devjoy.operator.environment.k8s.build.TriggerBindingActivationCondition;
import io.devjoy.operator.environment.k8s.build.TriggerTemplateActivationCondition;
import io.devjoy.operator.environment.k8s.build.WebhookSecretDependent;
import io.devjoy.operator.environment.k8s.deploy.AdditionalDeployResourcesConfigmapDependent;
import io.devjoy.operator.environment.k8s.deploy.ArgoActivationCondition;
import io.devjoy.operator.environment.k8s.deploy.ArgoCDDependentResource;
import io.devjoy.operator.environment.k8s.deploy.InitDeployPipelineDependent;
import io.devjoy.operator.environment.k8s.init.AdditionalResourceTaskDependent;
import io.devjoy.operator.environment.k8s.init.AdditionalResourcesConfigmapDependent;
import io.devjoy.operator.environment.k8s.init.CopyTaskDependent;
import io.devjoy.operator.environment.k8s.init.HelmCreateTaskDependent;
import io.devjoy.operator.environment.k8s.init.InitPipelineDependent;
import io.devjoy.operator.environment.k8s.status.ArgoCdStatus;
import io.devjoy.operator.environment.k8s.status.GiteaStatus;
import io.fabric8.kubernetes.api.model.APIGroup;
import io.fabric8.kubernetes.api.model.ConditionBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.ErrorStatusUpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Annotations;
import io.quarkiverse.operatorsdk.annotations.CSVMetadata.Provider;
import io.quarkiverse.operatorsdk.annotations.SharedCSVMetadata;
import io.quarkus.runtime.util.StringUtil;

@Workflow(
		dependents = {
			@Dependent(activationCondition = PipelineActivationCondition.class, type = BuildPipelineDependent.class),
			@Dependent(activationCondition = TriggerTemplateActivationCondition.class, type = BuildPushTriggerTemplateDependent.class),
			@Dependent(activationCondition = EventListenerActivationCondition.class, type = BuildEventListenerDependent.class),
			@Dependent(type = WebhookSecretDependent.class),
			@Dependent(type = GiteaDependentResource.class),
			@Dependent(activationCondition = TriggerBindingActivationCondition.class, type = GiteaPushTriggerBindingDependent.class),
			@Dependent(type = AdditionalResourcesConfigmapDependent.class),
			@Dependent(activationCondition = PipelineActivationCondition.class, type = InitPipelineDependent.class),
			@Dependent(activationCondition = PipelineActivationCondition.class, type = InitDeployPipelineDependent.class),
			@Dependent(activationCondition = ArgoActivationCondition.class, type = ArgoCDDependentResource.class),
			@Dependent(activationCondition = TaskActivationCondition.class, type = AdditionalResourceTaskDependent.class),
			@Dependent(type = AdditionalDeployResourcesConfigmapDependent.class),
			@Dependent(activationCondition = TaskActivationCondition.class, type = HelmCreateTaskDependent.class),
			@Dependent(activationCondition = TaskActivationCondition.class, type = CopyTaskDependent.class),
		})


@CSVMetadata(name = DevEnvironmentReconciler.CSV_METADATA_NAME, version = DevEnvironmentReconciler.CSV_METADATA_VERSION, displayName = "Devjoy Operator", description = "An operator to quickly create development environments and projects", provider = @Provider(name = "devjoy.io"), keywords = "Project,Quarkus,GitOps,Pipelines", annotations = @Annotations(repository = "https://github.com/Javatar81/devjoy", containerImage = DevEnvironmentReconciler.CSV_CONTAINER_IMAGE, others= {}))
public class DevEnvironmentReconciler implements Reconciler<DevEnvironment>, SharedCSVMetadata { 
  private static final String ARGOCD_VERSION_EXPECTED = "v1beta1";
  public static final String CSV_METADATA_VERSION = "0.2.0";
  public static final String CSV_METADATA_NAME = "devjoy-operator-bundle.v" + CSV_METADATA_VERSION;
  public static final String CSV_CONTAINER_IMAGE ="quay.io/devjoy/devjoy-operator:" + CSV_METADATA_VERSION;
  private static final Logger LOG = LoggerFactory.getLogger(DevEnvironmentReconciler.class);
  private final KubernetesClient client;
  
  public DevEnvironmentReconciler(KubernetesClient client) {
	  this.client = client;
	}

  @Override
  public UpdateControl<DevEnvironment> reconcile(DevEnvironment resource, Context<DevEnvironment> context) {
	
	LOG.info("Reconciling");
	DevEnvironment resourceToPatch = resourceForPatch(resource);
	if (resourceToPatch.getStatus() == null) {
		resourceToPatch.setStatus(new DevEnvironmentStatus());
	} 

	String status = resourceToPatch.getStatus().toString();
	
	var gitea = GiteaDependentResource.getResource(client, resourceToPatch).get();
	
	updateGiteaStatus(resourceToPatch);
	updateArgoCdStatus(resourceToPatch);
	if(resourceToPatch.getSpec() != null
		&& resourceToPatch.getSpec().getGitea() != null 
		&& resourceToPatch.getSpec().getGitea().isEnabled() 
		&& StringUtil.isNullOrEmpty(resourceToPatch.getSpec().getGitea().getResourceName())
		&& gitea != null) {
			resourceToPatch.getSpec().getGitea().setResourceName(gitea.getMetadata().getName());
		return UpdateControl.patchResourceAndStatus(resourceToPatch);
	} else if (!status.equals(resourceToPatch.getStatus().toString())) {
		return UpdateControl.patchStatus(resourceToPatch);
	} else {
		UpdateControl<DevEnvironment> noUpdate = UpdateControl.noUpdate();
		return noUpdate.rescheduleAfter(30, TimeUnit.SECONDS);
	}
	
  }

  private void updateGiteaStatus(DevEnvironment resource) {
	Gitea gitea = GiteaDependentResource.getResource(client, resource).get();
	GiteaStatus giteaStatus = resource.getStatus().getGitea();
	if (giteaStatus == null) {
		giteaStatus = new GiteaStatus();
		resource.getStatus().setGitea(giteaStatus);
	}
	List<String> versions = Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
		.withName("giteas.devjoy.io")
		.get())
		.map(CustomResourceDefinition::getSpec)
		.map(CustomResourceDefinitionSpec::getVersions)
		.orElse(Collections.emptyList())
		.stream()
			.map(CustomResourceDefinitionVersion::getName)
			.toList();
		
	giteaStatus.setAvailableGiteaApis(versions);
	giteaStatus.setExpectedGiteaApi(Gitea.API_VERSION);

	if (gitea != null) {
		giteaStatus.setResourceName(gitea.getMetadata().getName());
	}
  }

  private void updateArgoCdStatus(DevEnvironment resource) {
	ArgoCD argoCD = ArgoCDDependentResource.getResource(resource, client).get();
	LOG.debug("Checking status of argo. Available: {} ", argoCD != null);
	ArgoCdStatus argoStatus = resource.getStatus().getArgoCd();
	if (argoStatus == null) {
		argoStatus = new ArgoCdStatus();
		resource.getStatus().setArgoCd(argoStatus);
	}
	List<String> versions = Optional.ofNullable(client.apiextensions().v1().customResourceDefinitions()
		.withName(ArgoCD.getCRDName(ArgoCD.class))
		.get())
		.map(CustomResourceDefinition::getSpec)
		.map(CustomResourceDefinitionSpec::getVersions)
		.orElse(Collections.emptyList())
		.stream()
			.map(CustomResourceDefinitionVersion::getName)
			.toList();

	argoStatus.setAvailableArgoCDApis(versions);
	argoStatus.setExpectedArgoCDApi(ARGOCD_VERSION_EXPECTED);

	if (argoCD != null) {
		argoStatus.setHost(argoCD.getStatus().getHost());
		argoStatus.setPhase(argoCD.getStatus().getPhase());
		argoStatus.setResourceName(argoCD.getMetadata().getName());
	}
  }
  @Override
	public ErrorStatusUpdateControl<DevEnvironment> updateErrorStatus(DevEnvironment env, Context<DevEnvironment> context, Exception e) {
		LOG.info("Error of type {}", e.getClass());
		if (e.getCause() instanceof DevEnvironmentRequirementException) {
			DevEnvironmentRequirementException envNotFoundException = (DevEnvironmentRequirementException) e.getCause();
			env.getStatus().getConditions().add(new ConditionBuilder()
					.withObservedGeneration(env.getStatus().getObservedGeneration())
					.withType(DevEnvironmentConditionType.ENV_REQUIREMENT_NOT_FOUND.toString())
					.withMessage("Error")
					.withLastTransitionTime(LocalDateTime.now().toString())
					.withReason(envNotFoundException.getMessage())
					.withStatus("false")
					.build());
		}
		return ErrorStatusUpdateControl.patchStatus(env);
	}

	private DevEnvironment resourceForPatch(
		DevEnvironment original) {
		var res = new DevEnvironment();
		res.setMetadata(new ObjectMetaBuilder()
			.withName(original.getMetadata().getName())
			.withNamespace(original.getMetadata().getNamespace())
			.build());
		res.setSpec(original.getSpec());
		res.setStatus(original.getStatus());
		return res;
  	}

}

