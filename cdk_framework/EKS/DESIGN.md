# Purpose

The purpose of this directory is to deploy different EKS clusters using AWS CDK. 

# Architecture
![Deployment design](https://user-images.githubusercontent.com/54683946/183471629-59479f8c-db49-4c53-bbe5-48b5f18d6b14.png)


Steps in how the cluster deployment occurs:
1. Root construct, App, is created and calls cluster-deployment method
2. Cluster configuration file is read and validated and VPC stack is created. In it, the VPC is made and prepared to be passed in to each cluster. 
3. Cluster stacks are configured from the configuration file (by being casted to interfaces and validated), and the VPC is passed in as one of the props.
5. A single cluster is made in each stack with the configurations provided. The reason we need multiple stacks instead of putting all the clusters in one stack is because stack can’t hold more than one EKS cluster. 

## Directory Structure

```
/lib
    /config
        /cluster-config
            config.yml
    /interfaces
        cluster-interface
        ec2cluster-interface
    /stacks
        ec2-cluster-stack
        fargate-cluster-stack
        vpc-stack
    /utils
        validate-config-schema
        validate-interface-schema
    apps
    cluster-deployment
/test
```
`apps.ts` will call `cluster-deployment` to create all the stacks. The user either supplies the environment variable for the route to preferred configuraiton file or the default config file found in `/config/cluster-config` folder. `cluster-deployment.ts` will first validate the config file, then call `vpc-stack.ts` to create a VPC which will be passed in to each of the clusters specified in the configuration file. Then `resource-deployment` will cast each cluster defined in config file to either `ec2-cluster-stack` or `fargate-cluster-stack`. This stack will then create a cluster to be deployed. 

## Data Validation

There are 2 different validation steps:
1. Validate the configuration file. This is done by calling `validate-config-schema`. This validates that there are no added fields in the config file and that all the values for the defined fields are of type string. 
2. Once interfaces are created by the config file, each interface is passed into `validate-interface-schema` which checks:
    * `launch_type` and `version` need to be specified.
    * `launch_type ` must have 1 field - either `ec2` or `fargate` to specify what type of cluster will be made. 
    * If `launch_type` is `ec2`, then, `instance_type` is verified.
    * `instance_type` is verified to be compatible. Listings could be found at [Compatible Node Sizes](https://www.amazonaws.cn/en/ec2/instance-                types/). 
    * `version` needs to be between 1.18 to 1.21. Patch version releases are not supported by the CDK.

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

 ## Interfaces
 
 There are 2 different interfaces that could be made - `ClusterInterface` and `ec2ClusterInterface`. Every cluster in configuration file is first casted to `ClusterInterface`. `ClusterInterface` has 3 fields:
 * `name` - the name of the cluster
 * `launch_type` - the launch type of the cluster. Either `ec2` or `fargate`
 * `version` - the Kubernetes version of the cluster

Once the `ClusterInterface` is created, it checks to see what `launch_type` it has. If it is `fargate`, then nothing happens, but if it is `ec2`, you cast the cluster to `ec2ClusterInterface`. The reason for this is because there is an additional field called `instance_type` which is the instance type for the ec2 cluster node groups. 
 
 ## Cluster Stacks

There are 2 different stacks that could get potentially be created - `ec2ClusterStack` and `fargateClusterStack`. To determine which stack needs to be deployed, the ClusterInterface checks to see which `launch_type`. If it is `fargate`, it will deploy to `fargateClusterStack`, and if it is `ec2`, it will cast to `ec2ClusterInterface` and deploy to `ec2ClusterStack`. 

 There are a few props that need to be passed into the `fargate-cluster-stack.ts` file for the cluster to be created and deployed:
 * `name`. The name of the cluster.  
 * `vpc`. The VPC that was made in the `vpc-stack` is passed in. 
 * `version`. The Kubernetes Version needs to be specified.

Then, the stack will call eks.FargateCluster to create the cluster. It will look like this:

```
this.cluster = new eks.FargateCluster(this, props.name, {
      clusterName: props.name,
      vpc: props.vpc,
      version: props.version,
      clusterLogging: logging
    });
```
The vpc is the VPC that is passed in as a prop. The kubernetes version is the version that is passed in as a prop. Cluster logging types specify what type of logs we want the deployment to broadcast.

If `ec2-cluster-stack.ts` is called, there are some additional props that need to be passed:
* `name`. The name of the cluster.  
 * `vpc`. The VPC that was made in the `vpc-stack` is passed in. 
 * `version`. The Kubernetes Version needs to be specified.
 * `instance_type`. The instance type of the ec2 Cluster node groups. 

Then, the stack will call eks.Cluster to create the cluster. It will look like this:

```
this.cluster = new eks.Cluster(this, props.name, {
      clusterName: props.name,
      vpc: props.vpc,
      vpcSubnets: [{subnetType: ec2.SubnetType.PUBLIC}],
      defaultCapacity: 0,  // we want to manage capacity our selves
      version: props.version,
      clusterLogging: logging,
    
    });
```

The default capcity is 0, so it will start off with 0 node groups. This way we could add a node group of whatever launch type we want, instead of having the default node group `amd_64`.

Because we specified a default node capacity of 0, we need to add a NodeGroup. This is where we call `cluster.addNodeGroupCapacity` where we add the instanceType to the node group. Adding the node group will look like this:

```
this.cluster.addNodegroupCapacity('ng-' + instanceType, {
          instanceTypes: [new ec2.InstanceType(instace)],
          minSize: 2,
          nodeRole: workerRole
      })
```
The `instanceType` needs to be `m5`, `m6g` or `t4g` plus their compatible node size. The `minSize` is 2, which is the recommended minSize. `nodeRole` refers to the IAM Role that we want to assign to the node group. It is critical to provide the node group proper authorization so that the clusters can be properly managed, such as addign resources to these clusters. 

## Testing

### Fine-Grained Assertion Tests

These Tests were done in order to make sure the cloudformation template that was created for deployment has the right template, and will therefore provide the correct information. This is done for both the VPC stack and Cluster stacks. 

For testing the Cluster stack, the environment variable `CDK_CONFIG_PATH` is changed to be directed towards the `/test/test_config/test_clusters.yml` file. 
