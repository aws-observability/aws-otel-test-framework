import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { KubernetesVersion} from 'aws-cdk-lib/aws-eks';
import { ManagedPolicy, Role, ServicePrincipal } from 'aws-cdk-lib/aws-iam';

// not used right now but will be needed for when validation is done
const supportedNodeSizes = new Set(['medium', 'large', 'xlarge', '2xlarge', '4xlarge', '8xlarge', '12xlarge', '16xlarge', '24xlarge', 'metal']);
const supportedT4gInstances = new Set(['nano', 'micro', 'small', 'medium', 'large', 'xlarge', '2xlarge'])


export class EC2Stack extends Stack {
  cluster : eks.Cluster

  constructor(scope: Construct, id: string, props: EC2ClusterStackProps) {
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
    this.cluster.addNodegroupCapacity(`ng-${props.instance_type}`, {
        instanceTypes: [new ec2.InstanceType(props.instance_type)],
        minSize: 2,
        nodeRole: workerRole
    })
    this.cluster.awsAuth.addMastersRole(Role.fromRoleName(this, 'eks_admin_role', 'Admin'))
    this.cluster.awsAuth.addMastersRole(Role.fromRoleName(this, 'he', 'Admin'))

  }
}

export interface EC2ClusterStackProps extends StackProps{
    name: string;
    vpc: Vpc;
    version: KubernetesVersion;
    instance_type: string;
}
