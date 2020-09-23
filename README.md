# AOC IntegTest
## Run as Command Line

### Prerequisites

1. Configure aws credentials(.aws/credentials) on your host.
2. Setup the related resources(S3 bucket, iam roles, sshkey, security group) in your aws account by running this command

``
./gradlew :integ-test:run --args="setup"
``

this command will generate a file .aoc-stack.yml in the current work dir.
you can also provide .aoc-stack.yml in the current work dir before setup so that the setup command will create the resources base on your stack file, below is an example of the stack file(you will need to adjust the s3 bucket name as it's global unique and someone has already occupy it):

```
---
testingRegion: "us-west-2"
s3ReleaseCandidateBucketName: "aoc-release-candidate"
s3BucketName: "aws-opentelemetry-collector-test"
sshKeyS3BucketName: "aoc-ssh-key"
traceDataS3BucketName: "trace-expected-data"
```

### Run S3 Uploading

Firstly, you may want to upload the packages to s3 for testing.

run below command for uploading

````
./gradlew :integ-test:run --args="release -t=S3Release --local-packages-dir={the path to the packages you want to upload}"
````

an example for the local-package-dir, the VERSION file contains the package version like "v0.1.10".

````
local-packages
|-- debian
|   |-- amd64
|   |   `-- aws-opentelemetry-collector.deb
|   `-- arm64
|       `-- aws-opentelemetry-collector.deb
|-- linux
|   |-- amd64
|   |   `-- aws-opentelemetry-collector.rpm
|   `-- arm64
|       `-- aws-opentelemetry-collector.rpm
|-- windows
|   `-- amd64
|       `-- aws-opentelemetry-collector.msi
|-- GITHUB_SHA
|-- VERSION
`-- awscollector.tar
````

### Run EC2 Integ-test

````
./gradlew :integ-test:run --args="integ-test -t=EC2_TEST --package-version={the version you want to test}"
````

### Run ECS Integ-test with EC2 on Sidecar mode (EMF Metrics)
```
./gradlew :integ-test:run --args="integ-test -t=ECS_TEST -a=ECS_OPTIMIZED --package-version={the version you want to test} -e ecsLaunchType=EC2 -e ecsTaskDef=ECS_EC2_TEMPLATE"
```

### Run ECS Integ-test with Fargate on Sidecar mode (EMF Metrics)
```
./gradlew :integ-test:run --args="integ-test -t=ECS_TEST --package-version={the version you want to test} -e ecsLaunchType=FARGATE -e ecsTaskDef=ECS_FARGATE_TEMPLATE"
```

### Clean ECS testing resources
```
./gradlew :integ-test:run --args="clean -t=ECSClean --package-version={the version you want to test}"
```

### Run EKS Integ-test (EMF Metrics)
#### extra parameters:
* "-k eksClusterName": provide the name of EKS cluster on which run this test. (mandatory unless your specify kubeconfigPath)
* "-k kubectlPath": provide the path of kubectl binary to connect to your EKS cluster. (optional, if absent, a default binary will be downloaded)
* "-k kubeconfigPath": provide the path of kubeconfig. (optional, if absent, the kubeconfig will be generated according to your EKS cluster)
* "-k iamAuthenticatorPath": provide the path of aws-iam-authenticator binary to authenticate connection to your EKS cluster. (optional, if absent, a default binary will be downloaded)
* "-k eksTestManifestName": specify the test manifest under dir /templates/eks to deploy to your EKS cluster for testing. (optional, if absent, a default test manifest will be used)
```
./gradlew :integ-test:run --args="integ-test -t=EKS_TEST --package-version={the version you want to test} -k eksClusterName=my-cluster-name"
```

### Clean EKS testing resources
#### extra parameters:
* "-k eksClusterName": provide the name of EKS cluster on which run this test. (mandatory unless your specify kubeconfigPath)
* "-k kubectlPath": provide the path of kubectl binary to connect to your EKS cluster. (optional, if absent, a default binary will be downloaded)
* "-k kubeconfigPath": provide the path of kubeconfig. (optional, if absent, the kubeconfig will be generated according to your EKS cluster)
* "-k iamAuthenticatorPath": provide the path of aws-iam-authenticator binary to authenticate connection to your EKS cluster. (optional, if absent, a default binary will be downloaded)
```
./gradlew :integ-test:run --args="clean -t=EKSClean --package-version={the version you want to test} -k eksClusterName=my-cluster-name"
```

### Command Help

`
./gradlew :integ-test:run --args="-h"
`

`
./gradlew :integ-test:run --args="integ-test -h"
`

`
./gradlew :integ-test:run --args="release -h"
`

## Run as Github Action

### description

this action wraps the command lines, so it could be used in github workflow to perform integ-test and release for aoc.

### Inputs

#### `running_type`

value could be `integ-test`, `release`, `candidate`
value is `integ-test` by default

#### `opts`

the remaining options for command
the value is "-t=EC2Test -s=build/packages/.aoc-stack-test.yml" by default.

#### Example usage

```yaml
uses: wyTrivail/aocintegtest@master
with:
  running_type: integ-test
  opts: "-t=EC2Test -s=build/packages/.aoc-stack-test.yml"
```

## Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## License

This project is licensed under the Apache-2.0 License.

