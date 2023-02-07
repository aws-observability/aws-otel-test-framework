import {
  Stack,
  StackProps,
  aws_msk as msk,
  aws_ec2,
  RemovalPolicy
} from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { IVpc, Peer, Port, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';

// Configuration that is going to be used in the cluster
const SERVER_PROPERTIES = `auto.create.topics.enable = true
delete.topic.enable = true
log.retention.minutes = 5
`;

// Versions supported
const MSK_VERSIONS: string[] = ['3.2.0', '2.8.1'];

/** Stack that will concentrate all permanent MSK Cluster used in the integration
 * tests.
 */
export class MSKClustersStack extends Stack {
  constructor(scope: Construct, id: string, props: MSKStackProps) {
    super(scope, id, props);

    const configuration = new msk.CfnConfiguration(this, 'mskconfiguration', {
      name: 'testconfiguration',
      serverProperties: SERVER_PROPERTIES,
      kafkaVersionsList: MSK_VERSIONS
    });

    const securityGroupMSKAOC = this.createSecurityGroup(
      'aoc-msk',
      props.aocVPC
    );
    const securityGroupMSKEKS = this.createSecurityGroup(
      'eks-msk',
      props.eksVPC
    );

    MSK_VERSIONS.forEach((version) =>
      this.createCluster(
        'AOCMSKCluster',
        version,
        configuration,
        props.aocVPC,
        securityGroupMSKAOC
      )
    );
    MSK_VERSIONS.forEach((version) =>
      this.createCluster(
        'EKSMSKCluster',
        version,
        configuration,
        props.eksVPC,
        securityGroupMSKEKS
      )
    );
  }

  private createSecurityGroup(sgName: string, vpc: aws_ec2.IVpc) {
    const securityGroupMSKAOC = new SecurityGroup(this, `sg-${sgName}`, {
      vpc: vpc,
      securityGroupName: sgName
    });
    // plain text listener
    securityGroupMSKAOC.addIngressRule(
      Peer.ipv4(vpc.vpcCidrBlock),
      Port.tcp(9092)
    );
    // TLS listener
    securityGroupMSKAOC.addIngressRule(
      Peer.ipv4(vpc.vpcCidrBlock),
      Port.tcp(9094)
    );

    return securityGroupMSKAOC;
  }

  private createCluster(
    clusterPrefix: string,
    version: string,
    configuration: msk.CfnConfiguration,
    vpc: IVpc,
    securityGroup: aws_ec2.SecurityGroup
  ): msk.CfnCluster {
    const clusterName = `${clusterPrefix}${version.replaceAll('.', '-')}`;
    const logGroupName = `/aws-otel-tests/${clusterName}`;
    const logGroup = new LogGroup(this, `loggroup-${clusterName}`, {
      logGroupName: logGroupName,
      retention: RetentionDays.ONE_MONTH,
      removalPolicy: RemovalPolicy.DESTROY
    });

    return new msk.CfnCluster(this, `cluster-${clusterName}`, {
      clusterName: clusterName,
      kafkaVersion: version,
      numberOfBrokerNodes: 3,
      configurationInfo: {
        arn: configuration.attrArn,
        revision: 1
      },
      brokerNodeGroupInfo: {
        instanceType: 'kafka.t3.small',
        clientSubnets: vpc.privateSubnets.map((x) => x.subnetId),
        storageInfo: {
          ebsStorageInfo: {
            volumeSize: 10
          }
        },
        securityGroups: [securityGroup.securityGroupId]
      },
      clientAuthentication: {
        // No need to authenticate since we are only accessible inside the vpc
        unauthenticated: {
          enabled: true
        }
      },
      encryptionInfo: {
        encryptionInTransit: {
          clientBroker: 'TLS_PLAINTEXT'
        }
      },
      loggingInfo: {
        brokerLogs: {
          cloudWatchLogs: {
            enabled: true,
            logGroup: logGroup.logGroupName
          }
        }
      }
    });
  }
}

export interface MSKStackProps extends StackProps {
  eksVPC: IVpc; // VPC used in the EKS Clusters
  aocVPC: IVpc; // Legacy VPC used in the remaining in the tests
}
