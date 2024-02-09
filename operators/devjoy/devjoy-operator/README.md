# Devjoy Operator
The Devjoy operator allows the creation of development environments and projects.

## Installation
First, you need to add the devjoy catalog source to your OpenShift cluster. This can be done as follows:

`oc apply -f deploy/devjoy-catalog-source.yaml`

Second, create a new project for the operator, e.g. devjoy-gitea-operator

Third, navigate to the OperatorHub in your cluster and search for 'Devjoy'. Install the operator into the project you created in the previous step. Because the operator requires CRDs from the Gitea operator, the OLM will automatically install it. 

## Custom Resources 

### DevEnvironment

The DevEnvironment custom resource represents a whole development environment including a Git server (Gitea), ArgoCD and a set of pipelines.

```yml
    apiVersion: devjoy.io/v1alpha1
kind: DevEnvironment
metadata:
  name: test-env
  namespace: myproject
spec:
  gitea:
    enabled: true
    managed: true
    resourceName: test-env-gitea
  mavenSettingsPvc: maven
```

The following table describes some of the core fields of the DevEnvironment custom resource:

| Field                         | Description |
| -----------                   | ----------- |
| gitea                         | The field to configure the Gitea repository.       |
| gitea.enabled                 | Whether the gitea repository shall be used. Only true is supported at the moment because Gitea is the only supported Git server.      |
| gitea.managed                 | If the Gitea repository is managed by this operator or if you want to manage it yourself. Only true is fully implemented at the moment       |
| mavenSettingsPvc              | The name of the persistent volume claim that is used as cache for the maven settings and repository. If left blank, no cache is used. This is only used for pipelines.         |

### Project

The Project custom resource uses a DevEnvironment to create a new project.

```yml
apiVersion: devjoy.io/v1alpha1
kind: Project
metadata:
  name: testproj
  namespace: myproject
spec:
  environmentName: test-env
  environmentNamespace: myproject
  owner:
    user: testuser
    userEmail: testuser@example.com
  quarkus:
    enabled: true
    extensions:
      - quarkus-resteasy-reactive-jackson
      - quarkus-jdbc-postgresql
```

The following table describes some of the core fields of the DevEnvironment custom resource:

| Field                         | Description |
| -----------                   | ----------- |
| environmentName               | The reference to the environment name. By default the project namespace.       |
| environmentNamespace          | The reference to the environment namespace. By default the environment in the same namespace.      |
| owner                         | The owner of the project. Can be a user or an organization. The latter is currently not supported.      |
| owner.user                    | The username of the owning user. Can be empty when organization is specified.         |
| owner.userEmail               | The user email of the owning user. If empty one will be generated.       |
| owner.organization            | The organization owning the project (not supported at the moment). If left empty a user must be set.      |
| quarkus                       | The quarkus configuration if this is a quarkus project.         |
| quarkus.enabled               | If it is a Quarkus project. Must be true because only Quarkus is supported.       |
| quarkus.extensions            | An array of extensions.      |
| existingRepositoryCloneUrl    | If the project should reside in an external repo not managed by Devjoy then this must be set (not supported at the moment)

