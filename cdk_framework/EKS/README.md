# Welcome to your CDK Test Framework

This is the repository for running tests using AWS CDK. 

## Repo Structure

There are a number of important files that are used when running the CDK. 

The `lib` directory is where all the cluster deployment implementation is done. `app.ts` is where the root construct, app, is created and calls cluster deployment and resource deployment. 

The `test` directory is where testing is done on the cluster deployment. 

The `cdk.json` file tells the CDK Toolkit how to execute your app.

The `Makefile` is a file used to deploy the clusters in parallel. All that is required is call `make EKS-infra` and all the clusters configured in configuration file will be deployed. 

The `package.json` file contains the important libraries that are needed to run the deployment. 

The `tsconfig.json` file is used to tell TypeScript how to configure the project. 

## Getting Started

### Environment Setup

Since the code base in written in TypeScript, the CDK library has to be downloaded using Node. 

1. Make sure you have Node, so that you can use `npm` control. 
2. Download from EKS directory the AWS CDK Library by typing `npm install aws-cdk-lib`. 
3. In order to use the linter, the eslint dependency needs to be downloaded. This could be done by calling `npm install --save-dev eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin`. 

### Environemnt Variables

There are a number of environment variables that should be defined before deploying the clusters:

* `CDK_CONFIG_PATH` - This is the path for which the cluster configuration file is located - default is `clusters.yml` in `lib/config/cluster-config` folder.
* `REGION` - This is the region for which the clusters should be deployed - default is `us-west-2`.

### Setting Config File

Sample template of what config file looks like could be seen in the YAML files found in `lib/config` folder. Should create a category called `clusters`, where each desired cluster should be configured. The name of the cluster given should be the key name for each cluster. Then, there are a couple of fields that need to be addressed:

* `clusters`:
    * `launch_type` - choose either `ec2` or `fargate` subcategory - can't be both. Determines the launch type for the cluster to be deployed. This will act as the key to another list. 
        * `ec2_instance` - This is the the CPU Architecture for `ec2` launch types. It is only useful information for `ec2` key. If the `launch_type` is `fargate`, then nothing will happen by providing an `ec2_instance`. The options are `m6g`, `t4g`, amd `m5`, otherwise, an error will be thrown. There can’t be any other characters. It is case insensitive.
        * `node_size` - This determines the size of the cpu architecture (memory, vCPUs, etc). It is only useful information for `ec2` key. If the key is `fargate` nothing will happen by providing the `node_size`. The list of compatible sizes could be found here: [Compatible Node Sizes](https://www.amazonaws.cn/en/ec2/instance-types/). It is case insensitive.
    * `version` - Kubernetes Version. Supported Kubernetes versions are any versions between 1.18-1.21. This can be seen at [KubernetesVersion API](https://docs.aws.amazon.com/cdk/api/v2/docs/aws-cdk-lib.aws_eks.KubernetesVersion.html). Additionally, specifying patch releases isn't an option as the CDK doesn't support it. Therefore, every input value must be the minor version. 

Here is a sample configuration file example:
```
---
clusters:
  amdCluster:
    launch_type: 
      ec2:
        ec2_instance: m6g
        node_size: large
    version: 1.21
  fargateCluster:
    launch_type: 
      fargate:
    version: 1.21
  t4gCluster:
    launch_type: 
      ec2:
        ec2_instance: t4g
        node_size: large
    version: 1.21
```

There are three different clusters being deployed in this example - amdCluster, fargateCluster, and t4gCluster. There are only 2 fields for each cluster - `launch_type` and `version`. Then, in `launch_type`, either `ec2` or `fargate` is specified. If `fargate` is specified, then it should be left empty as shown above. If `ec2` is specified, then both `ec2_instance` and `node_size` should be defined. 
   

### Deploying clusters

1. Call `cdk synth` to synthesize the clusters to be deployed. This also makes sure everything was configured properly. 
2. Call `cdk deploy --all` to dpeloy all the clusters. You could specify the cluster to deploy by the name calling `cdk deploy CLUSTERNAME`. CLUSTERNAME is the name of the cluster you passed in. 

#### Makefile

The makefile is used to deploy the clusters in parallel. It determines how many clusters are configured, and then sets that number to the number of processes to run to deploy all the clusters simultaneously. In order to accomplish this, after saving the configuration file and setting all appropriate envrionment variables, call `make EKS-infra` and all the clusters should be deployed. This call will first call `deploy-VPC` which will call `cdk synth` and then create the VPC by calling `cdk deploy EKSVpc`. After the VPC is deployed, it then calls `deploy-clusters` where all the EKS clusters that were configured are deployed. The way it calls `deploy-clusters` is by calling `make -j #processes deploy-clusters`, where #processes is the number of clusters being deployed. 

Additionally, in order to destroy all the clusters in parallel call `make destroy-EKS-infra`. 

## Testing

There are two different tests that are implemented:
1. Fine-Grained Assertion Tests
    * These tests are used to test the template of the the cloudformation stacks that are being created. 
2. Unit Tests
    * These tests are created to ensure proper configuraiton validation. These are accomplished by using the table-driven approach. 

In order to run these tests, use command `npm test`. 

### Linter

ESLint is used to make sure formatting is done well. To run the linter, call `npm run lint`. 

## Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npm run test`    perform the jest unit tests
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk synth`       emits the synthesized CloudFormation template
