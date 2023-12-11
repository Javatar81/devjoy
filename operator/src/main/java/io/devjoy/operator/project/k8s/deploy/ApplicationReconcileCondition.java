package io.devjoy.operator.project.k8s.deploy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Optional;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.devjoy.gitea.domain.ApiAccessMode;
import io.devjoy.gitea.k8s.Gitea;
import io.devjoy.gitea.repository.k8s.GiteaRepository;
import io.devjoy.operator.project.k8s.Project;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.OpenShiftAPIGroups;
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
       Optional<GiteaRepository> giteaRepo = context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator);
       return giteaRepo
        .filter(r -> r.getStatus() != null)
        .map(r -> {    
            String cloneUrl = ApiAccessMode.INTERNAL.toString().equals(accessMode) ? r.getStatus().getInternalCloneUrl() : r.getStatus().getCloneUrl();
            try {
                URI uri = new URI(cloneUrl.replace(".git", "/raw/branch/main/bootstrap/argo-application.yaml"));
                var con = (HttpURLConnection) uri.toURL().openConnection();
                con.connect();
                if (200 == con.getResponseCode()) {
                    return true;
                } else {
                    LOG.warn("Cannot read {} response code is {}", uri, con.getResponseCode());
                    return false;
                }
            } catch (URISyntaxException | IOException e) {
                LOG.error("Error with repo raw uri", e);
                return null;
            }
       }).orElse(false);
    }
}