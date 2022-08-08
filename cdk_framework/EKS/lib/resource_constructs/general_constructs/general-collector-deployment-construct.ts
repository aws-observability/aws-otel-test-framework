import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from '../specific_constructs/namespace-construct';
import { GRPCServiceConstruct } from '../specific_constructs/grpc-service-construct';
import { ServiceAccountConstruct } from '../specific_constructs/service-account-construct';
import { CollectorConfigMapConstruct } from '../specific_constructs/collector-config-map-construct';
import { CollectorDeploymentConstruct } from '../specific_constructs/collector-deployment-construct';
import { TCPServiceConstruct } from '../specific_constructs/tcp-service-construct';
import { UDPServiceConstruct } from '../specific_constructs/udp-service-construct';

//TODO - Consider renaming. Role is a different Kubernetes kind so 
// this name is confusing
export class GeneralCollectorDeploymentConstruct extends Construct{
    collectorRole: Construct

    constructor(scope: Construct, id: string, props: GeneralCollectorDeploymentConstructProps) {
        super(scope, id);
        const collectorAppLabel = 'collector'

        if (props.deployServices){
            if (props.grpcServiceName == undefined) {
                throw new Error('No GRPC Service name provided')
            }
            if (props.grpcPort == undefined) {
                throw new Error('No GRPC port provided')
            }
            if (props.udpServiceName == undefined) {
                throw new Error('No UDP Service name provided')
            }
            if (props.udpPort == undefined) {
                throw new Error('No UDP port provided')
            }
            if (props.tcpServiceName == undefined) {
                throw new Error('No TCP Service name provided')
            }
            if (props.httpPort == undefined) {
                throw new Error('No HTTP port provided')
            }
            new GRPCServiceConstruct(this, 'grpc-service-construct', {
                cluster: props.cluster,
                name: props.grpcServiceName,
                namespaceConstruct: props.namespaceConstruct,
                appLabel: collectorAppLabel,
                grpcPort: props.grpcPort
            })
            new UDPServiceConstruct(this, 'udp-service-construct', {
                cluster: props.cluster,
                name: props.udpServiceName,
                namespaceConstruct: props.namespaceConstruct,
                appLabel: collectorAppLabel,
                udpPort: props.udpPort
            })
            new TCPServiceConstruct(this, 'tcp-service-construct', {
                cluster: props.cluster,
                name: props.tcpServiceName,
                namespaceConstruct: props.namespaceConstruct,
                appLabel: collectorAppLabel,
                httpPort: props.httpPort
            })
        }

        const serviceAccountName = `collector-service-account`
        const serviceAccountConstruct = new ServiceAccountConstruct(this, 'service-account-construct', {
            cluster: props.cluster,
            name: serviceAccountName,
            namespaceConstruct: props.namespaceConstruct
        })

        const collectorConfigMapConstruct = new CollectorConfigMapConstruct(this, 'collector-config-map-construct', {
            cluster: props.cluster,
            namespaceConstruct: props.namespaceConstruct,
            collectorConfig: props.collectorConfig
        })

        new CollectorDeploymentConstruct(this, 'collector-deployment-construct', {
            cluster: props.cluster,
            namespaceConstruct: props.namespaceConstruct,
            collectorAppLabel: collectorAppLabel,
            serviceAccountConstruct: serviceAccountConstruct,
            collectorConfigMapConstruct: collectorConfigMapConstruct
        })
    }
}

export interface GeneralCollectorDeploymentConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    collectorConfig: string
    deployServices: boolean
    grpcServiceName?: string
    grpcPort?: number
    udpServiceName?: string
    udpPort?: number
    tcpServiceName?: string
    httpPort?: number
}