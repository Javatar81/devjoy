See https://docs.openshift.com/container-platform/4.14/operators/admin/olm-managing-custom-catalogs.html#olm-managing-custom-catalogs-fb

# Initial 
opm init -c alpha gitea-operator -oyaml > catalog/gitea-operator-bundle/index.yaml
opm render quay.io/devjoy/gitea-operator-bundle:0.1.0 --output=yaml >> catalog/gitea-operator-bundle/index.yaml

# When updating
opm render quay.io/devjoy/gitea-operator-bundle:latest -o yaml > catalog/gitea-operator-bundle/index.yaml
opm render quay.io/devjoy/gitea-operator-bundle:next -o yaml >> catalog/gitea-operator-bundle/index.yaml

# Validate
opm validate catalog

# Generate dockerfile
opm generate dockerfile catalog -i registry.redhat.io/openshift4/ose-operator-registry:v4.14