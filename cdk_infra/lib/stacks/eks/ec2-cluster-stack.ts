import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2 } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { Vpc, InstanceType } from 'aws-cdk-lib/aws-ec2';
import {
  KubernetesVersion,
  Nodegroup,
  NodegroupAmiType
} from 'aws-cdk-lib/aws-eks';
import { ManagedPolicy } from 'aws-cdk-lib/aws-iam';
import { GetLayer } from '../../utils/eks/kubectlLayer';
import { StringParameter } from 'aws-cdk-lib/aws-ssm';

function getReleaseVersion(amiType: string, clusterVersion: string, scope: Construct): string {
  let parameterName: string;

  // https://docs.aws.amazon.com/eks/latest/userguide/retrieve-ami-id.html
  if (amiType === NodegroupAmiType.AL2_X86_64) {
    parameterName = `/aws/service/eks/optimized-ami/${clusterVersion}/amazon-linux-2/recommended/release_version`;
  } else if (amiType === NodegroupAmiType.AL2_ARM_64) {
    parameterName = `/aws/service/eks/optimized-ami/${clusterVersion}/amazon-linux-2-arm64/recommended/release_version`;
  } else if (amiType === NodegroupAmiType.AL2023_X86_64_STANDARD) {
    parameterName = `/aws/service/eks/optimized-ami/${clusterVersion}/amazon-linux-2023/x86_64/standard/recommended/release_version`;
  } else if (amiType === NodegroupAmiType.AL2023_ARM_64_STANDARD) {
    parameterName = `/aws/service/eks/optimized-ami/${clusterVersion}/amazon-linux-2023/arm64/standard/recommended/release_version`;
  } else {
    throw new Error(`Unsupported amiType: ${amiType}`);
  }

  // Fetch and return the release version from the SSM parameter
  return StringParameter.fromStringParameterAttributes(scope, `NodeGroupReleaseVersion-${clusterVersion}-${amiType}`, {
    parameterName,
  }).stringValue;
}

export class EC2Stack extends Stack {
  cluster: eks.Cluster;

  constructor(scope: Construct, id: string, props: EC2ClusterStackProps) {
    super(scope, id, props);

    const logging = [
      eks.ClusterLoggingTypes.API,
      eks.ClusterLoggingTypes.AUDIT,
      eks.ClusterLoggingTypes.AUTHENTICATOR,
      eks.ClusterLoggingTypes.CONTROLLER_MANAGER,
      eks.ClusterLoggingTypes.SCHEDULER
    ];

    this.cluster = new eks.Cluster(this, props.name, {
      clusterName: props.name,
      vpc: props.vpc,
      vpcSubnets: [{ subnetType: ec2.SubnetType.PUBLIC }],
      defaultCapacity: 0, // we want to manage capacity ourselves
      version: props.version,
      clusterLogging: logging,
      kubectlLayer: GetLayer(this, props.version)
    });

    const lt = new ec2.LaunchTemplate(this, `${props.name}-launch-template`, {
      requireImdsv2: true,
      httpEndpoint: true,
      httpPutResponseHopLimit: 2,
      httpTokens: ec2.LaunchTemplateHttpTokens.REQUIRED
    });

    const clusterNodeGroup = new Nodegroup(this, `${props.name}-managed-ng`, {
      amiType: props.amiType,
      releaseVersion: getReleaseVersion(props.amiType, props.version.version, this),
      instanceTypes: props.instanceTypes,
      cluster: this.cluster,
      minSize: 2,
      maxSize: 4,
      desiredSize: 3,
      subnets: { subnetType: ec2.SubnetType.PUBLIC },
      launchTemplateSpec: {
        id: lt.launchTemplateId as string,
        version: lt.latestVersionNumber
      }
    });

    clusterNodeGroup.role.addManagedPolicy(
      ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore')
    );
  }
}

export interface EC2ClusterStackProps extends StackProps {
  name: string;
  vpc: Vpc;
  version: KubernetesVersion;
  instanceTypes: InstanceType[];
  amiType: NodegroupAmiType;
}
