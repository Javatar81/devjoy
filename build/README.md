# Prerequisites 

    kubectl create secret generic my-push-creds \
    --from-file=.dockerconfigjson=<path/to/.docker/config.json> \
    --type=kubernetes.io/dockerconfigjson

    oc set data secret/redhat-pull-secret --from-file=.dockerconfigjson=pull-secret.txt

Associate the secret with the service account running the pipeline (by default 'pipeline').

    apiVersion: v1
    kind: ServiceAccount
    metadata:
    name: build-bot
    secrets:
    - name: my-push-creds

Reference the service account via serviceAccountName in the task run
    apiVersion: tekton.dev/v1beta1
    kind: TaskRun
    metadata:
    name: build-with-basic-auth
    spec:
    serviceAccountName: build-bot
    steps: