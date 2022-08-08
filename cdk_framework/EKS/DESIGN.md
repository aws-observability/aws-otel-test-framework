# Purpose

The purpose of this directory is to deploy EKS clusters using AWS CDK. 

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
