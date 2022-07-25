import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { KubernetesVersion} from 'aws-cdk-lib/aws-eks';
import { ManagedPolicy, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';


export class ClusterStack extends Stack {
  cluster : eks.Cluster | eks.FargateCluster

  constructor(scope: Construct, id: string, props: ClusterStackProps) {
    super(scope, id, props);

    const logging = [
      eks.ClusterLoggingTypes.API,
      eks.ClusterLoggingTypes.AUDIT,
      eks.ClusterLoggingTypes.AUTHENTICATOR,
      eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
      eks.ClusterLoggingTypes.SCHEDULER,
    ]

    const workerRole = new Role(this, 'EKSWorkerRole', {
      assumedBy: new ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName("AmazonPrometheusRemoteWriteAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSXrayWriteOnlyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("CloudWatchAgentAdminPolicy"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonS3ReadOnlyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AWSAppMeshEnvoyAccess"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKSWorkerNodePolicy"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEC2ContainerRegistryReadOnly"),
        ManagedPolicy.fromAwsManagedPolicyName("AmazonEKS_CNI_Policy")
      ]
    });

    if(props.launchType === 'ec2'){
      this.cluster = new eks.Cluster(this, props.name+'-Cluster', {
      clusterName: props.name,
      vpc: props.vpc,
      vpcSubnets: [{subnetType: ec2.SubnetType.PUBLIC}],
      defaultCapacity: 0,  // we want to manage capacity our selves
      version: props.version,
      clusterLogging: logging,
      
    });
      if(props.cpu === "arm_64"){
          this.cluster.addNodegroupCapacity('ng-arm', {
              instanceTypes: [new ec2.InstanceType('m6g.' + props.nodeSize)],
              minSize: 2,
              nodeRole: workerRole
          })
      } else {
          this.cluster.addNodegroupCapacity('ng-amd', {
              instanceTypes: [new ec2.InstanceType('m5.' + props.nodeSize)],
              minSize: 2,
              nodeRole: workerRole
          })
      }
    }

    if(props.launchType === 'fargate'){
      console.log("Fargate starting")
      this.cluster = new eks.FargateCluster(this, props.name + '-Cluster', {
        clusterName: props.name,
        vpc: props.vpc,
        version: props.version,
        clusterLogging: logging
      });
    }

    this.cluster.awsAuth.addMastersRole(Role.fromRoleName(this,"eks-admin-role","Admin"))

  }
}

export interface ClusterStackProps extends StackProps{
    launchType: string;
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
    cpu: string
    nodeSize: string
}