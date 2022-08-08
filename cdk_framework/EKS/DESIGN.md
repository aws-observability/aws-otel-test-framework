# Purpose

The purpose of this directory is to deploy different EKS clusters using AWS CDK. 

# Architecture
![Deployment design](https://user-images.githubusercontent.com/54683946/183471629-59479f8c-db49-4c53-bbe5-48b5f18d6b14.png)


Steps in how the cluster deployment occurs:
1. Root construct, App, is created and the configuration file is read
2. Calls deployClusters method where VPC stack is created. In it, the VPC is made and prepared to be passed in to each cluster. 
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
 The VPC was configured based on what was done in the [terraform framework](https://github.com/aws-observability/aws-otel-test-framework/blob/6cd6478ce2c32223494460b390f33aeb5e61c48e/terraform/eks_fargate_setup/main.tf#:~:text=%23%20%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D%2D-,module%20%22vpc%22%20%7B,-source%20%3D%20%22). The same configuration is done for every EKS cluster, so it was fine making this the default. The only difference between the terraform framework and CDK is that the CIDR blocks for the subnets aren’t the same. This is fine as long as they both remain in the range in the VPC CIDR block. Providing the option to configure one’s own VPC is out of the scope of the project for the time being. 
