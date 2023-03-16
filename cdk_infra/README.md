# Welcome to your CDK Test Framework

This CDK Project handles the deployment of long lived infrastructure used by the ADOT testing framework.
Among the long lived infrastructure, there are:
* VPC for EKS Clusters
* EKS Clusters with different characteristics, defined in `lib/config/cluster-config/clusters.yml`
* MSK Clusters

## Getting Started

### Environment Setup

Since the code base in written in TypeScript, the CDK library has to be downloaded using Node. 

1. Make sure you have Node, so that you can use `npm` control. 
2. `npm i`

### Deploying the infrastructure

1. Build or update a cluster config file. See sections below for configuration options.
2. `make deploy-infra` to deploy VPC and clusters.

#### Updating Clusters

In order to update clusters, just change the config file and then call `make deploy-infra`. There are some limitations in what could be updated:
1. Can't downgrade a version. 
2. Can't change launch_types without changing Cluster name

### Cluster Configuration

The following is only valid for the EKS Clusters.

* `clusters`:
    * `name` - `string` The name of the cluster. Cluster names must be unique.
    * `launch_type` - `ec2` or `fargate` - Determines the launch type for the cluster to be deployed. Case insensitive.
    * `version` - Kubernetes Version. Supported Kubernetes versions are any versions between 1.21-1.23. This can be seen at [KubernetesVersion API](https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_eks.KubernetesVersion.html). Additionally, specifying patch releases isn't an option as the CDK doesn't support it. Therefore, every input value must be the minor version.
    * `instance_type` - `string` - Only required for EC2 clusters.See here for a list of [compatible node sizes](https://www.amazonaws.cn/en/ec2/instance-types/). Example: `m5.large`.

Sample configuration file:
```
---
clusters:
  - name: ec2Cluster
    version: "1.21"
    launch_type: ec2
    instance_type: m5.large
  - name: fargateCluster
    version: "1.20"
    launch_type: fargate
    cert_manager: true
```

### Environment Variables

Environment variables that should be defined before deploying the clusters:

* `CDK_CONFIG_PATH` - This is the path for which the cluster configuration file is located - default is `clusters.yml` in `lib/config/cluster-config` folder.

### Example Deployment

Here is an example case of how to run a deployment. Let's say there are two clusters that one desires to deploy, and amd_64 cluster (ec2_instance type is `m5`), with node size "`large`" and a fargate cluster. So, first thing to do is to set up the configuration file. Looking at the example provided above, all we have to do is give copy the template above and give the clusters names. For this example, we will use `ec2Cluster` for the ec2 cluster and `fargateCluster` for the fargate cluster.

```
---
clusters:
  - name: ec2Cluster
    version: "1.21"
    launch_type: ec2
    instance_type: m5.large
  - name: fargateCluster
    version: "1.20"
    launch_type: fargate
```
Now that we have the configuration file set up, we want to make sure the `CDK_CONFIG_PATH` environment variable is set up to route to this configuration file. This only needs to be done if the clusters.yml file in /lib/config/cluster-config folder was not overridden. Once the variable is set, all that needs to be done is call `make deploy-infra` and all the clusters will be deployed.

## Testing

1. Fine-Grained Assertion Tests
    * These tests are used to test the template of cloudformation stacks that are being created. 

In order to run these tests, use command `npm run test`. 

### Linter

ESLint is used to make sure formatting is done well. To run the linter, call `npm run lint`. 

### cdk.context.json

The app relies on `CDK_DEFAULT_ACCOUNT` and `CDK_DEFAULT_REGION` to set their [environment](https://docs.aws.amazon.com/cdk/v2/guide/environments.html). The CDK documentation suggestions that [runtime context](https://docs.aws.amazon.com/cdk/v2/guide/context.html#context_construct) files should be checked into source control. A readonly IAM role is required to run `cdk synth` on all accounts that this app will be subsequently deployed to. 

## Useful commands

* `npm run build` -  compile typescript to js
* `npm run watch` -  watch for changes and compile
* `npm run test`  -  perform the jest unit tests
* `npm run cdk synth`   -    emits the synthesized CloudFormation template
* `npm run cdk deploy`  -    deploy this stack to your default AWS account/region
* `npm run cdk diff`    -    compare deployed stack with current state
