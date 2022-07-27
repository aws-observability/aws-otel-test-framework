# Welcome to your CDK Test Framework

This is the repository for running tests using AWS CDK. 

## Repo Structure

There are a number of important files that are used when running the CDK. 

The `lib` directory is where all the cluster deployment implementation is done. `app.ts` is where the root construct, app, is created and calls cluster deployment and resource deployment. 

The `test` directory is where testing is done on the cluster deployment. 

The `cdk.json` file tells the CDK Toolkit how to execute your app.

The `package.json` file contains the important libraries that are needed to run the deployment. 

The `tsconfig.json` file is used to tell TypeScript how to configure the project. 

## Getting Started

### Environment Setup

Since the code base in written in TypeScript, the CDK library has to be downloaded using Node. 

1. Make sure you have Node, so that you can use `npm` control. 
2. Download the AWS CDK Library by typing `npm install aws-cdk-lib`. 

### Environemnt Variables

There are a number of environment variables that should be defined before deploying the clusters:

* `CDK_CONFIG_PATH` - This is the path for which the cluster configuration file is located - default is `clusters.yml` in `lib/config` folder.
* `REGION` - This is the region for which the clusters should be deployed - default is `us-west-2`.

### Setting Config File

Sample template of what config file looks like could be seen in the YAML files found in `lib/config` folder. Should create a category called `clusters`, where each desired cluster should be configured. The name of the cluster given should be the key name for each cluster. Then, there are a couple of fields that need to be addressed:

* `clusters`:
    * `launch_type` - choose either `ec2` or `fargate` subcategory - can't be both. Determines the launch type for the cluster to be deployed. This will act as the key to another list. 
        * `ec2_instance` - This is the the CPU Architecture for `ec2` launch types. It is only useful information for `ec2` key. If the `launch_type` is `fargate`, then nothing will happen by providing an `ec2_instance`. The options are `m6g`, `t4g`, amd `m5`, otherwise, an error will be thrown. There can’t be any other characters. It is case insensitive.
        * `node_size` - This determines the size of the cpu architecture (memory, vCPUs, etc). It is only useful information for `ec2` key. If the key is `fargate` nothing will happen by providing the `node_size`. The list of compatible sizes could be found here: https://www.amazonaws.cn/en/ec2/instance-types/. It is case insensitive.
    * `version` - Kubernetes Version. Supported Kubernetes versions are anything between 1.18-1.21.
   

### Deploying clusters

1. Call `cdk synth` to synthesize the clusters to be deployed. This also makes sure everything was configured properly. 
2. Call `cdk deploy --all` to dpeloy all the clusters. You could specify the cluster to deploy by the name calling `cdk deploy CLUSTERNAME`. CLUSTERNAME is the name of the cluster you passed in. 

## Testing

In order to run tests, use command `tsc && npm test`. 

## Useful commands

* `npm run build`   compile typescript to js
* `npm run watch`   watch for changes and compile
* `npm run test`    perform the jest unit tests
* `cdk deploy`      deploy this stack to your default AWS account/region
* `cdk diff`        compare deployed stack with current state
* `cdk synth`       emits the synthesized CloudFormation template
