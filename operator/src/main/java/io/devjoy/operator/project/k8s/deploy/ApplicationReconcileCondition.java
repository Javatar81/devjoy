package io.devjoy.operator.project.k8s.deploy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ApiAccessMode;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.operator.project.k8s.Project;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ApplicationReconcileCondition implements Condition<Application, Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationReconcileCondition.class);
    private final GitopsRepositoryDiscriminator gitopsRepoDiscriminator = new GitopsRepositoryDiscriminator();
    String accessMode;

    public ApplicationReconcileCondition() {
        accessMode = ConfigProvider.getConfig().getConfigValue("io.devjoy.gitea.api.access.mode").getValue();
    }
    
    @Override
    public boolean isMet(DependentResource<Application, Project> dependentResource, Project primary, Context<Project> context) {
       LOG.warn("Checking whether argo application shall be reconciled.");
       Optional<GiteaRepository> giteaRepo = context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator);
       return giteaRepo
        .filter(r -> r.getStatus() != null)
        .map(r -> ApiAccessMode.INTERNAL.toString().equals(accessMode) ? r.getStatus().getInternalCloneUrl() : r.getStatus().getCloneUrl())
        .filter(url -> url != null)
        .map(url -> {    
            try {
                URI uri = new URI(url.replace(".git", "/raw/branch/main/bootstrap/argo-application.yaml"));
                var con = (HttpURLConnection) uri.toURL().openConnection();
                con.connect();
                if (200 == con.getResponseCode()) {
                    LOG.info("Argo application will be reconciled");
                    return true;
                } else {
                    LOG.warn("Cannot read {} response code is {}", uri, con.getResponseCode());
                    return false;
                }
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error with repo raw uri", e);
                return null;
            }
       }).orElseGet(() -> {
        LOG.warn("Repo does not yet exist");
        return false;
       });
    }
}