import { Stack, StackProps, aws_eks as eks} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Role } from 'aws-cdk-lib/aws-iam';
import { GetLayer } from '../utils/kubectlLayer';
import { Vpc } from 'aws-cdk-lib/aws-ec2';
import { KubernetesVersion } from 'aws-cdk-lib/aws-eks';


export class FargateStack extends Stack {
  cluster : eks.FargateCluster

  constructor(scope: Construct, id: string, props: FargateClusterStackProps) {
    super(scope, id, props);

    const logging = [
      eks.ClusterLoggingTypes.API,
      eks.ClusterLoggingTypes.AUDIT,
      eks.ClusterLoggingTypes.AUTHENTICATOR,
      eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
      eks.ClusterLoggingTypes.SCHEDULER,
    ]
    this.cluster = new eks.FargateCluster(this, props.name, {
      clusterName: props.name,
      vpc: props.vpc,
      version: props.version,
      clusterLogging: logging,
      kubectlLayer: GetLayer(this,props.version.version),

    });
    this.cluster.awsAuth.addMastersRole(Role.fromRoleName(this, 'eks_admin_role', 'Admin'))

  }
}

export interface FargateClusterStackProps extends StackProps{
  name: string;
  vpc: Vpc;
  version: KubernetesVersion;
}
