import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { KubernetesVersion} from 'aws-cdk-lib/aws-eks';
import { ManagedPolicy, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';

const supportedNodeSizes = new Set(['medium', 'large', 'xlarge', '2xlarge', '4xlarge', '8xlarge', '12xlarge', '16xlarge', '24xlarge', 'metal']);
const supportedT4gInstances = new Set(['nano', 'micro', 'small', 'medium', 'large', 'xlarge', '2xlarge'])


export class EC2Stack extends Stack {
  cluster : eks.Cluster | eks.FargateCluster

  constructor(scope: Construct, id: string, props: EC2ClusterStackProps) {
    super(scope, id, props);

    const logging = [
      eks.ClusterLoggingTypes.API,
      eks.ClusterLoggingTypes.AUDIT,
      eks.ClusterLoggingTypes.AUTHENTICATOR,
      eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
      eks.ClusterLoggingTypes.SCHEDULER,
    ]

    const ec2Instance = props.ec2_instance.toLowerCase()
    const nodeSize = validateNodeSize(props.node_size, ec2Instance)

    const workerRole = new Role(this, 'EKSWorkerRole', {
      assumedBy: new ServicePrincipal('ec2.amazonaws.com'),
      managedPolicies: [
        ManagedPolicy.fromAwsManagedPolicyName('AmazonPrometheusRemoteWriteAccess'),
        ManagedPolicy.fromAwsManagedPolicyName('AWSXrayWriteOnlyAccess'),
        ManagedPolicy.fromAwsManagedPolicyName('CloudWatchAgentAdminPolicy'),
        ManagedPolicy.fromAwsManagedPolicyName('AmazonS3ReadOnlyAccess'),
        ManagedPolicy.fromAwsManagedPolicyName('AWSAppMeshEnvoyAccess'),
        ManagedPolicy.fromAwsManagedPolicyName('AmazonEKSWorkerNodePolicy'),
        ManagedPolicy.fromAwsManagedPolicyName('AmazonEC2ContainerRegistryReadOnly'),
        ManagedPolicy.fromAwsManagedPolicyName('AmazonEKS_CNI_Policy')
      ]
    });
    this.cluster = new eks.Cluster(this, props.name, {
    clusterName: props.name,
    vpc: props.vpc,
    vpcSubnets: [{subnetType: ec2.SubnetType.PUBLIC}],
    defaultCapacity: 0,  // we want to manage capacity our selves
    version: props.version,
    clusterLogging: logging,
    
    });
    this.cluster.addNodegroupCapacity('ng-' + ec2Instance, {
        instanceTypes: [new ec2.InstanceType(ec2Instance + '.' + nodeSize)],
        minSize: 2,
        nodeRole: workerRole
    })
    this.cluster.awsAuth.addMastersRole(Role.fromRoleName(this, 'eks_admin_role', 'Admin'))

  }
}

export interface EC2ClusterStackProps extends StackProps{
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
    ec2_instance: string;
    node_size: string
}


function validateNodeSize(size: string, instance: string){
    if(!size || size === null || size === 'null' || size === ''){
        return null
    }
    const adjustedSize = size.toLowerCase()

    if(instance === 't4g'){
        if(!supportedT4gInstances.has(adjustedSize)){
            throw new Error('Node size is not one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/')
        }
    } else {
        if(!supportedNodeSizes.has(adjustedSize)){
            throw new Error('Node size is not one of the options listed here https://www.amazonaws.cn/en/ec2/instance-types/')
        }
        if(instance === 'm5' && adjustedSize === 'medium'){
            throw new Error('CPU architecture and node size are not compatible')
        }
        if(instance === 'm6g' && adjustedSize === '24xlarge'){
            throw new Error('CPU architecture and node size are not compatible')
        }
    }
    
    return adjustedSize

}

