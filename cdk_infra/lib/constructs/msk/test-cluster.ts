import { Construct } from 'constructs';
import { IVpc, Peer, Port, SecurityGroup } from 'aws-cdk-lib/aws-ec2';
import { aws_msk, RemovalPolicy } from 'aws-cdk-lib';
import { LogGroup, RetentionDays } from 'aws-cdk-lib/aws-logs';

export interface MSKTestClusterProps {
  readonly namePrefix: String;
  readonly kafkaVersion: string;
  readonly vpc: IVpc;
  // Kafka server.properties file https://kafka.apache.org/documentation/#configuration
  readonly serverProperties: string;
}

/**
 * Minimal set of components required to create a MSK cluster that can be used
 * in integration tests.
 * It will generate a cluster whose name is: <namePrefix><dash separated version>. E.g.:
 * EKSMSKCluster2-8-1
 */
export class MSKTestCluster extends Construct {
  constructor(scope: Construct, id: string, props: MSKTestClusterProps) {
    super(scope, id);

    const versionName = props.kafkaVersion.replaceAll('.', '-');
    // Name following the naming conversion
    const clusterName = `${props.namePrefix}${versionName}`;

    const configuration = new aws_msk.CfnConfiguration(
      this,
      `config-${clusterName}`,
      {
        name: `config-${clusterName}`,
        serverProperties: props.serverProperties,
        kafkaVersionsList: [props.kafkaVersion]
      }
    );

    const securityGroup = new SecurityGroup(
      this,
      `sg-${props.namePrefix}-${versionName}`,
      {
        vpc: props.vpc,
        securityGroupName: clusterName
      }
    );
    // plain text listener
    securityGroup.addIngressRule(
      Peer.ipv4(props.vpc.vpcCidrBlock),
      Port.tcp(9092)
    );
    // TLS listener
    securityGroup.addIngressRule(
      Peer.ipv4(props.vpc.vpcCidrBlock),
      Port.tcp(9094)
    );

    const logGroupName = `/aws-otel-tests/${clusterName}`;
    const logGroup = new LogGroup(this, `loggroup-${clusterName}`, {
      logGroupName: logGroupName,
      retention: RetentionDays.ONE_MONTH,
      removalPolicy: RemovalPolicy.DESTROY
    });

    new aws_msk.CfnCluster(this, `cluster-${clusterName}`, {
      clusterName: clusterName,
      kafkaVersion: props.kafkaVersion,
      numberOfBrokerNodes: 3,
      configurationInfo: {
        arn: configuration.attrArn,
        revision: 1
      },
      brokerNodeGroupInfo: {
        instanceType: 'kafka.t3.small',
        clientSubnets: props.vpc.privateSubnets.map((x) => x.subnetId),
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
