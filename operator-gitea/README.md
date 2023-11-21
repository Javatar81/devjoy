# Devjoy Gitea Operator

 With the Devjoy Gitea Operator you can create and manage instances of [Gitea](https://about.gitea.com), a lightweight DevOps platform. For each instance of Gitea, the operator will also manage a Postgresql database. This project is inspired by: https://github.com/redhat-gpte-devopsautomation/gitea-operator.

The Devjoy Gitea Operator is based on Quarkus and the Java Operator SDK. It is implemented to run on OpenShift 4.13+.  


## Installation
First, you need to add the devjoy catalog source to your OpenShift cluster. This can be done as follows:

`oc apply -f deploy/gitea-catalog-source.yaml`

Second, create a new project for the operator, e.g. devjoy-gitea-operator

Third, navigate to the OperatorHub in your cluster and serch for 'Gitea'. Install the operator into the project you created in the previous step. 

## Custom Resources 

### Gitea

The Gitea custom resource represents an instance of Gitea with a Postgres database running in OpenShift.

```yml
    apiVersion: devjoy.io/v1alpha1
    kind: Gitea
    metadata:
        name: mygitea
    spec:
      logLevel: WARN
      resourceRequirementsEnabled: false
      ingressEnabled: true
      adminEmail: devjoyadmin@example.com
      adminPasswordLength: 12
      adminUser: devjoyadmin
```

The following table describes some of the core fields of the Gitea custom resource:

| Field                         | Description |
| -----------                   | ----------- |
| adminUser                     | The name of the admin user.       |
| adminEmail                    | The email of the admin user.       |
| adminPassword                 | The optional admin password. If not set it will be generated. Once set the value will be moved to a secret.        |
| adminPasswordLength           | The length of the generated admin password. Value is ignored if adminPassword is set. Min length is 10.            |
| resourceRequirementsEnabled   | Enables resource requirements such as cpuLimit, cpuRequest, memoryLimit, and memoryRequest. Default value is true. |
| memoryLimit                   | The memory resource limits for the Gitea deployment.                                                               |
| memoryRequest                 | The memory resource requests for the Gitea deployment.                                                             |
| cpuLimit                      | The cpu resource limits for the Gitea deployment.                                                                  |
| cpuRequest                    | The cpu resource requests for the Gitea deployment.                                                                |
| logLevel                      | The log level for Gitea. Default is Warn                                                                           |
| ingressEnabled                | Create a route / ingress to access for Gitea.                                                                      |


### GiteaRepository

The GiteaRepository custom resource represents a repository in Gitea. Each repository is owned by a user that will also be created with the repository.

```yml
    apiVersion: devjoy.io/v1alpha1
    kind: GiteaRepository
    metadata:
        name: myrepo
    spec:
       deleteOnFinalize: true
       user: testuser
       visibility: PUBLIC
```

The following table describes some of the core fields of the GiteaRepository custom resource:

| Field                         | Description |
| -----------                   | ----------- |
| user                          | The user owning the repository.       |
| visibility                    | The visibility of the repository: PRIVATE (default) or PUBLIC.     |
| deleteOnFinalize              | Whether the repository should be deleted with the repository resource. Default is true.    |
| webhooks                      | Webhooks for the repository.            |


## Background Infos
This project has been generated with operator-sdk 1.25.3:

    operator-sdk init --plugins quarkus --domain devjoy.io --project-name devjoy-generator
    operator-sdk create api --version v1alpha1 --kind Gitea
    operator-sdk create api --version v1alpha1 --kind GiteaRepository
    

