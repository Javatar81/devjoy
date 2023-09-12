# Setup
This project has been generated with operator-sdk 1.25.3:

    operator-sdk init --plugins quarkus --domain devjoy.io --project-name devjoy-generator 
    operator-sdk create api --version v1alpha1 --kind Repository
    
# Release
mvn clean package -Dquarkus.container-image.build=true -Dquarkus.container-image.push=true
podman build -t quay.io/devjoy/gitea-operator-bundle -f target/bundle/gitea-bundle/bundle.Dockerfile 
podman push quay.io/devjoy/gitea-operator-bundle
opm index add --bundles quay.io/devjoy/gitea-operator-bundle:latest --tag quay.io/devjoy/gitea-operator-catalog --build-tool podman    
podman push quay.io/devjoy/gitea-operator-catalog
# Docs
Inspired by: https://github.com/redhat-gpte-devopsautomation/gitea-operator