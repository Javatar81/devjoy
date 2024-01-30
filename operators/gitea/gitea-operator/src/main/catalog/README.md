See https://docs.openshift.com/container-platform/4.14/operators/admin/olm-managing-custom-catalogs.html#olm-managing-custom-catalogs-fb

# Initial 
opm init -c alpha gitea-operator -oyaml > gitea-operator-catalog/index.yaml
opm render quay.io/devjoy/gitea-operator-bundle:0.1.0 --output=yaml >> gitea-operator-catalog/index.yaml

# When updating
opm render quay.io/devjoy/gitea-operator-bundle:latest -o yaml > gitea-operator-catalog/index.yaml
opm render quay.io/devjoy/gitea-operator-bundle:next --output=yaml >> gitea-operator-catalog/index.yaml

# Validate
opm validate gitea-operator-catalog

# Generate dockerfile
opm generate dockerfile catalog -i registry.redhat.io/openshift4/ose-operator-registry:v4.14