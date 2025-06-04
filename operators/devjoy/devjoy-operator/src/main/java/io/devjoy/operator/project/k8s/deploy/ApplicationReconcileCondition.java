package io.devjoy.operator.project.k8s.deploy;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Optional;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.argoproj.v1alpha1.Application;
import io.devjoy.gitea.repository.k8s.model.GiteaRepository;
import io.devjoy.gitea.util.ApiAccessMode;
import io.devjoy.operator.project.k8s.Project;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

public class ApplicationReconcileCondition implements Condition<Application, Project> {
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationReconcileCondition.class);
    private final GitopsRepositoryDiscriminator gitopsRepoDiscriminator = new GitopsRepositoryDiscriminator();
    String accessMode;
    boolean trustAll;
    private TrustManager[] trustAllCerts = new TrustManager[]{
        new TrustAllTrustManager()
    };

    public ApplicationReconcileCondition() {
        accessMode = ConfigProvider.getConfig().getConfigValue("io.devjoy.gitea.api.access.mode").getValue();
        trustAll = Boolean.valueOf(ConfigProvider.getConfig().getConfigValue("quarkus.tls.trust-all").getValue());
    }

    
    
    
    @Override
    public boolean isMet(DependentResource<Application, Project> dependentResource, Project primary, Context<Project> context) {
       LOG.warn("Checking whether argo application shall be reconciled.");
       LOG.info("Using access mode: {}", accessMode);
       Optional<GiteaRepository> giteaRepo = context.getSecondaryResource(GiteaRepository.class, gitopsRepoDiscriminator);
       return giteaRepo
        .filter(r -> r.getStatus() != null)
        .map(r -> ApiAccessMode.INTERNAL.toString().equals(accessMode) ? r.getStatus().getInternalCloneUrl() : r.getStatus().getCloneUrl())
        .filter(url -> url != null)
        .map(url -> {    
            try {
                URI uri = new URI(url.replace(".git", "/raw/branch/main/bootstrap/argo-application.yaml"));
                var con = (HttpURLConnection) uri.toURL().openConnection();
                LOG.warn("Trust all is {}", trustAll);
                if (trustAll && con instanceof HttpsURLConnection secureCon) {
                    LOG.warn("Using insecure HTTPS connection. Only use this in dev mode!");
                    SSLContext sslTrustAll = SSLContext.getInstance("SSL");
                    sslTrustAll.init(null, trustAllCerts, new java.security.SecureRandom());
                    secureCon.setSSLSocketFactory(sslTrustAll.getSocketFactory());
                } 
                con.connect();
                if (200 == con.getResponseCode()) {
                    LOG.info("Argo application will be reconciled");
                    return true;
                } else {
                    LOG.warn("Cannot read {} response code is {}", uri, con.getResponseCode());
                    return false;
                }
            } catch (URISyntaxException | IOException | KeyManagementException | NoSuchAlgorithmException e) {
                LOG.error("Error with repo raw uri", e);
                return null;
            }
       }).orElseGet(() -> {
        LOG.warn("Repo does not yet exist");
        return false;
       });
    }
}