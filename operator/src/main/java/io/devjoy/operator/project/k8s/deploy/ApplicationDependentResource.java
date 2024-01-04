package io.devjoy.operator.project.k8s.deploy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ApiAccessMode;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.operator.environment.k8s.DevEnvironment;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent(labelSelector = ApplicationDependentResource.LABEL_TYPE_SELECTOR)
public class ApplicationDependentResource extends CRUDKubernetesDependentResource<Application, Project>{
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDependentResource.class);
    public static final String LABEL_KEY = "devjoy.io/argo.type";
	public static final String LABEL_VALUE = "deploy-argo-app";
	static final String LABEL_TYPE_SELECTOR = LABEL_KEY + "=" + LABEL_VALUE;
    private final GitopsRepositoryDiscriminator gitopsRepoDiscriminator = new GitopsRepositoryDiscriminator();
    
    @ConfigProperty(name = "io.devjoy.gitea.api.access.mode")
    String accessMode;
    
    public ApplicationDependentResource() {
        super(Application.class);
    }

    @Override
    protected Application desired(Project primary, Context<Project> context) {
       LOG.debug("Setting desired state."); 
       Optional<GiteaRepository> giteaRepo = context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator);
       return giteaRepo.map(r -> {
            String cloneUrl = ApiAccessMode.INTERNAL.toString().equals(accessMode) ? r.getStatus().getInternalCloneUrl() : r.getStatus().getCloneUrl();
            try {
                URI uri = new URI(cloneUrl.replace(".git", "/raw/branch/main/bootstrap/argo-application.yaml"));
                 Application argo = context.getClient().resources(Application.class)
                .load(uri.toURL().openStream())
                .item();
            //argo.getMetadata().setNamespace(getOwningEnvironment(primary).map(e -> e.getMetadata().getNamespace())
            //    .orElseGet(() -> primary.getMetadata().getNamespace()));
            argo.getMetadata().setNamespace(primary.getMetadata().getNamespace());
            argo.getMetadata().setName(getName(primary));
            HashMap<String, String> labels = new HashMap<>();
            labels.put(LABEL_KEY, LABEL_VALUE);
            argo.getMetadata().setLabels(labels);
            return argo;
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error with repo raw uri", e);
                return null;
            }
       }).orElse(null);
    }

    private Optional<DevEnvironment> getOwningEnvironment(Project owningProject, KubernetesClient client) {
		return Optional.ofNullable(
				client.resources(DevEnvironment.class).inNamespace(owningProject.getSpec().getEnvironmentNamespace())
						.withName(owningProject.getSpec().getEnvironmentName()).get());
	}

    public static String getName(Project primary) {
        return "argocd-" + primary.getMetadata().getName();
    }
}


