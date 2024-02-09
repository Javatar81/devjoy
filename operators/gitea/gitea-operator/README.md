# Devjoy Gitea Operator

 With the Devjoy Gitea Operator you can create and manage instances of [Gitea](https://about.gitea.com), a lightweight DevOps platform. For each instance of Gitea, the operator will also manage a Postgresql database. This project is inspired by: https://github.com/redhat-gpte-devopsautomation/gitea-operator.

The Devjoy Gitea Operator is based on Quarkus and the Java Operator SDK. It is implemented to run on OpenShift 4.13+.  


## Installation
First, you need to add the devjoy catalog source to your OpenShift cluster. This can be done as follows:

`oc apply -f deploy/gitea-catalog-source.yaml`

Second, create a new project for the operator, e.g. devjoy-gitea-operator

Third, navigate to the OperatorHub in your cluster and search for 'Gitea'. Install the operator into the project you created in the previous step. 

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

The following table describes all available fields of the Gitea custom resource:

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
| allowCreateOrganization       | Allow new users to create organizations by default. Default is false.                                              |
| configOverrides               | Allows to override arbitrary config properties in the app.ini.                                                     |
| configOverrides.section.key | Override a key in a specific app.ini section. Example configOverrides.database.HOST. More infos [here](https://docs.gitea.com/administration/config-cheat-sheet) |
| disableRegistration           | Disable registration, after which only admin can create accounts for users. Default value is false.                |
| enableCaptcha                 | Enable this to use captcha validation for registration. Default value is false.                                    |
| image                         | The image url to use for Gitea. Default is quay.io/gpte-devops-automation/gitea                                    |
| imageTag                      | The image tag to use for Gitea. Default is 1.20                                                                    |
| mailer                        | The specification for the mailer.                                                                                  |
| mailer.enableNotifyMail       | Enable this to ask for mail confirmation of registration.                                                          |
| mailer.enabled                | Enable to use a mail service.                                                                                      |
| mailer.from                   | Mail from address, RFC 5322. This can be just an email address, or the "Name" \email@example.com\ format.          |
| mailer.heloHostname           | HELO hostname. If empty it is retrieved from system.                                                               |
| mailer.host                   | Mail server address + port. e.g. smtp.gmail.com. For smtp+unix, this should be a path to a unix socket instead. Mail server port. If no protocol is specified, it will be inferred by this setting.                                                                  |
| mailer.password               | Password of mailing user. Use `your password` for quoting if you use special characters in the password.           |
| mailer.protocol               | Mail server protocol. One of "smtp", "smtps", "smtp+starttls", "smtp+unix", "sendmail", "dummy"                    |
| mailer.user                   | Username of mailing user (usually the sender's e-mail address).                                                    |
| postgres                      | The specification for the postgres database.                                                                       |
| postgres.cpuLimit             | The cpu resource limits for the Postgres deployment.                                                               |
| postgres.cpuRequest           | The cpu resource requests for the Postgres deployment.                                                             |
| postgres.memoryLimit          | The memory resource limits for the Postgres deployment.                                                            |
| postgres.memoryRequest        | The memory resource requests for the Postgres deployment.                                                          |
| postgres.image                | The image to be used for the Postgres pod.                                                                         |
| postgres.imageTag             | The image tag to be used for the Postgres pod.                                                                     |
| postgres.ssl                  | Enables SSL for database connections.                                                                              |
| postgres.storageClass         | The storage class used to store the Postgres data.                                                                 |
| postgres.volumeSize           | The size of the volume to store Postgres data.                                                                     |
| registerEmailConfirm          | Enable this to ask for mail confirmation of registration. Requires Mailer to be enabled.. Default value is false.  |
| route                         | The hostname of the route. If not set it will be generated by OpenShift.                                           |
| ssl                           | Enables SSL for Gitea.                                                                                             |
| sso                           | Enables SSO using RHSSO and OpenShift.                                                                             |
| storageClass                  | Storage class of the persistent volume claim.                                                                      |
| volumeSize                    | Size of the storage request of the persistent volume claim. Default value is 4Gi.                                  |


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

The following table describes all of the available fields of the GiteaRepository custom resource:

| Field                         | Description |
| -----------                   | ----------- |
| user                          | The user owning the repository.       |
| visibility                    | The visibility of the repository: PRIVATE (default) or PUBLIC.     |
| deleteOnFinalize              | Whether the repository should be deleted with the repository resource. Default is true.    |
| webhooks                      | Array of webhooks for the repository.            |
| webhooks.active               | Wether this webhook is active. Default is true.            |
| webhooks.branchFilter         | The branch to trigger the webhook, e.g. main or * for all branches.           |
| webhooks.events               | The list of events to trigger the webhook, e.g. push  |
| webhooks.httpMethod           | The http method of the webhook. Either POST or GET |
| webhooks.secretRef            | A secret containing the secret of the webhook. By default the user secret is referred. |
| webhooks.secretRef.namespace  | The namespace of the referenced secret. If empty, it points to the same namespace as the repository resource. |
| webhooks.secretRef.name       | The name of the referenced secret. |
| webhooks.secretRef.key        | The key storing the password as value. |
| webhooks.targetUrl            | The target url of the webhook. |
| webhooks.type                 | The type of the webhook. One of dingtalk, discord, gitea, gogs, msteams, slack, telegram, feishu, wechatwork, packagist. |

## Background Infos
This project has been generated with operator-sdk 1.25.3:

    operator-sdk init --plugins quarkus --domain devjoy.io --project-name devjoy-generator
    operator-sdk create api --version v1alpha1 --kind Gitea
    operator-sdk create api --version v1alpha1 --kind GiteaRepository
    

