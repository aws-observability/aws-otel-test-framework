import { aws_ec2, Stack, StackProps } from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { IVpc } from 'aws-cdk-lib/aws-ec2';
import { MSKTestCluster } from '../../constructs/msk/test-cluster';

// Configuration that is going to be used in the cluster
const SERVER_PROPERTIES = `auto.create.topics.enable = true
delete.topic.enable = true
log.retention.minutes = 5
`;

// Versions supported for the integration tests
const MSK_VERSIONS: string[] = ['3.2.0', '2.8.1'];

/** Stack that will concentrate all permanent MSK clusters used in the integration
 * tests. It will create MSK Clusters for all supported versions of Kafka and in
 * all supported VPCs.
 *
 * Supported VPCs:
 *  * AOC VPC - Legacy VPC, created through terraform
 *  * EKS VPC - Used by the EKS clusters
 *
 * In the tests where these clusters are used, we strongly rely on naming
 * conventions so that it is possible to fetch information about these clusters
 * in the terraform modules that run the tests.
 * The convention is to name the cluster with  <prefix><dash separated version>.
 * E.g.: AOCMSKCluster2-8-1
 */
export class MSKClustersStack extends Stack {
  constructor(scope: Construct, id: string, props: MSKStackProps) {
    super(scope, id, props);

    // Get the legacy VPC that was created with terraform
    // This lookup has to be done inside a stack
    const aocVPC = props.aocVPC == null ? aws_ec2.Vpc.fromLookup(this, 'aoc-vpc', {
      vpcName: 'aoc-vpc'
    }) : props.aocVPC;

    // Legacy VPC clusters
    MSK_VERSIONS.forEach(
      (version) =>
        new MSKTestCluster(this, `aoc${version.replaceAll('.', '-')}`, {
          kafkaVersion: version,
          namePrefix: 'AOCMSKCluster',
          serverProperties: SERVER_PROPERTIES,
          vpc: aocVPC
        })
    );

    // EKS VPC clusters
    MSK_VERSIONS.forEach(
      (version) =>
        new MSKTestCluster(this, `eks${version.replaceAll('.', '-')}`, {
          kafkaVersion: version,
          namePrefix: 'EKSMSKCluster',
          serverProperties: SERVER_PROPERTIES,
          vpc: props.eksVPC
        })
    );
  }
}

export interface MSKStackProps extends StackProps {
  eksVPC: IVpc; // VPC used in the EKS Clusters
  aocVPC?: IVpc; // Used in tests only
}
