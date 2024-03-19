package io.devjoy.operator.project.k8s.deploy;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.argoproj.v1alpha1.Application;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.util.ApiAccessMode;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDNoGCKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@KubernetesDependent()
public class ApplicationDependent extends CRUDNoGCKubernetesDependentResource<Application, Project>{
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationDependent.class);
    private final GitopsRepositoryDiscriminator gitopsRepoDiscriminator = new GitopsRepositoryDiscriminator();
    
    @ConfigProperty(name = "io.devjoy.gitea.api.access.mode")
    String accessMode;
    
    public ApplicationDependent() {
        super(Application.class);
    }

    @Override
    protected Application desired(Project primary, Context<Project> context) {
       LOG.info("Setting desired state."); 
       Optional<GiteaRepository> giteaRepo = context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator);
       return giteaRepo.map(r -> {
            String cloneUrl = ApiAccessMode.INTERNAL.toString().equals(accessMode) ? r.getStatus().getInternalCloneUrl() : r.getStatus().getCloneUrl();
            try {
                URI uri = new URI(cloneUrl.replace(".git", "/raw/branch/main/bootstrap/argo-application.yaml"));
                Application app = context.getClient().resources(Application.class)
                    .load(uri.toURL().openStream())
                    .item();
                LOG.info("Loaded state from git {}.", uri); 
                return app;
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error with repo raw uri", e);
                return null;
            }
       }).orElse(null);
    }

    public static Resource<Application> getResource(KubernetesClient client, Project project) {
        String namespace = project.getOwningEnvironment(client).map(e -> e.getMetadata().getNamespace()).orElseGet(() -> project.getMetadata().getNamespace());
		return client.resources(Application.class).inNamespace(namespace)
				.withName(project.getMetadata().getName());
	}

}


