# Purpose

The purpose of this directory is to deploy different EKS clusters using AWS CDK. 

# Architecture
![Deployment design](https://user-images.githubusercontent.com/54683946/183471629-59479f8c-db49-4c53-bbe5-48b5f18d6b14.png)


Steps in how the cluster deployment occurs:
1. Root construct, App, is created and calls cluster-deployment method
2. Cluster configuration file is read and VPC stack is created. In it, the VPC is made and prepared to be passed in to each cluster. 
3. Cluster stacks are configured from the configuration file and the VPC is passed in as one of the props.
4. A single cluster is made in each stack with the configurations provided. The reason we need multiple stacks instead of putting all the clusters in one stack is because stack can’t hold more than one EKS cluster 

## Directory Structure

```
/lib
    /config
        /cluster-config
            config.yml
    /stacks
        cluster-stack
        vpc-stack
    /utils
        validate-cluster-config
    apps
    cluster-deployment
/test
```
`apps.ts` will call `cluster-deployment` to create all the stacks. `cluster-deployment.ts` will first call `vpc-stack.ts` to create a VPC which will be passed in to each of the clusters specified in the configuration file. The user either supplies the environment variable for the route to preferred configuraiton file or the default config file found in `/config/cluster-config` folder. The configuration file is validated by being passed into `validate-cluster-config.ts`. Then `resource-deployment` will call `cluster-stack` for every cluster in configuration file. The `cluster-stack` will then create the cluster to be deployed.  

## Data Validation

After following the directions of configuring the configuration file (found in README), the following is checked in `validate-cluster-config`:
* `launch_type` and `version` need to be specified.
* `launch_type ` must have 1 field - either `ec2` or `fargate` to specify what type of cluster will be made. 
* If `launch_type` is `ec2`, then, only fields allowed are `ec2_instance` and `node_size`.
* `ec2_instance` and `node_size` have to be compatible. Listings could be found at [Compatible Node Sizes](https://www.amazonaws.cn/en/ec2/instance-types/). 
* `version` needs to be between 1.18 to 1.21. Patch version releases are not supported by the CDK. 
* Clusters can't be given the same name

## VPC

A default VPC is created by implemeting:
```
const vpc = new ec2.Vpc(this, 'EKSVpc',
    {cidr: "10.0.0.0/16",
    natGateways: 1,
    vpnGateway: true,
    availabilityZones: ['us-west-2a', 'us-west-2b', 'us-west-2c'],
    subnetConfiguration: [
    {
        cidrMask: 24,
        subnetType: ec2.SubnetType.PRIVATE_WITH_NAT,
        name: 'private_subnet'
    },
    {
        cidrMask: 24,
        subnetType: ec2.SubnetType.PUBLIC,
        name: "public_subnet"
    }
    ]
});
```
 The VPC was configured based on what was done in the [terraform framework](https://github.com/aws-observability/aws-otel-test-framework/blob/6cd6478ce2c32223494460b390f33aeb5e61c48e/terraform/eks_fargate_setup/main.tf#:~:text=%23%20%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D-,module%20%22vpc%22%20%7B,-source%20%3D%20%22). The same configuration is done for every EKS cluster, so it was fine making this the default. The only difference between the terraform framework and CDK is that the CIDR blocks for the subnets aren’t the same. This is fine as long as they both remain in the range in the VPC CIDR block.

 ## Cluster-Stacks

 There are a few props that need to be passed into the `cluster-stack.ts` file for the cluster to be created and deployed:
 * VPC. The VPC that was made in the `vpc-stack` is passed in. 
 * Launch Type. It has to specify whether the cluster will be `ec2` or `fargate` cluster. If `ec2` cluster, then, the `ec2_instance` and `node_size` needs to be speified as well. 
 * Kubernetes Version. The Kubernetes Version needs to be specified. 
 * Cluster Name. 

 When `cluster-stack.ts` is called from `resource-deployment` with all the props configured, it first checks to see what type of laucnh type the cluster is. If it ec2, then an `eks.Cluster` is created, whereas, if it is fargate, then an `eks.FargateCluster` is created. 
 
 If an `eks.Cluster` is created, then the cluster will be created by passing in the following:
 * vpc
 * kubernetes version
 * default node capacity
 * cluster logging types. 
 
 The vpc is the VPC that is passed in as a prop. The default capcity is 0, so it will start off with 0 node groups. This way we could add a node group of whatever launch type we want, instead of having the default node group amd_64. The kubernetes version is the version that is passed in as a prop. Cluster logging types specify what type of logs we want the deployment to broadcast. The cluster creation will look like this:

```
this.cluster = new eks.Cluster(this, props.name, {
        clusterName: props.name,
        vpc: props.vpc,
        defaultCapacity: 0,  // we want to manage capacity our selves
        version: props.version,
        clusterLogging: logging,
      
      });
```

Because we specified a default node capacity of 0, we need to add a NodeGroup. This is where we call `cluster.addNodeGroupCapacity` where we add the instanceType to the node group. Adding the node group will look like this:

```
this.cluster.addNodegroupCapacity('ng-' + instanceType, {
          instanceTypes: [new ec2.InstanceType(instace)],
          minSize: 2,
          nodeRole: workerRole
      })
```
The `instanceType` needs to be `m5`, `m6g` or `t4g` plus their compatible node size. The `minSize` is 2, which is the recommended minSize. `nodeRole` refers to the IAM Role that we want to assign to the node group. It is critical to provide the node group proper authorization so that the clusters can be properly managed, such as addign resources to these clusters. 

The other option for cluster creation was the `eks.FargateCluster`. The FargateCluster has the same instantion as the EC2 Cluster, except that there is no need to specify defaultCapacity, which in turn means there is no need to add a node group. 

## Testing

There are two types of tests that were created - Fine-Grained Assertion Tests and Unit Tests. 

### Fine-Grained Assertion Tests

These Tests were done in order to make sure the cloudformation template that was created for deployment has the right template, and will therefore provide the correct information. This is done for both the VPC stack and Cluster stacks. 

For testing the Cluster stack, the environment variable `CDK_CONFIG_PATH` is changed to be directed towards the `/test/test_config/test_clusters.yml` file. 

### Unit Testing

The unit testing is used to make sure the `validate-cluster-config.ts` file is properly validating the configuration file. 