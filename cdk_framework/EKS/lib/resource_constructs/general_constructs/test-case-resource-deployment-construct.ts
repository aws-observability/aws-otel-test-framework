import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from '../specific_constructs/namespace-construct';
import { GeneralSampleAppDeploymentConstruct, GeneralSampleAppDeploymentConstructProps } from './general-sample-app-deployment-construct';
import { GeneralCollectorDeploymentConstruct, GeneralCollectorDeploymentConstructProps } from './general-collector-deployment-construct';

export class TestCaseResourceDeploymentConstruct extends Construct {
    name: string
    namespace: Construct

    constructor(scope: Construct, id: string, props: TestCaseResourceDeploymentConstructProps){
        super(scope, id);

        // namespace deployment
        const namespaceName = `collector-namespace`
        const collectorNamespaceConstruct = new NamespaceConstruct(this, 'collector-namespace-construct', {
            cluster: props.cluster,
            name: namespaceName
        })

        // used for when sample app is push mode
        const deployServices = props.sampleAppMode === 'push'
        const grpcServiceName = 'collector-grpc'
        const grpcPort = 4317
        const udpServiceName = 'collector-udp'
        const udpPort = 55690
        const tcpServiceName = 'collector-tcp'
        const httpPort = 4318

        // sample app deployment
        const sampleAppLabel = props.sampleAppMode === 'push' ? 'push-mode-sample-app' : 'pull-mode-sample-app'
        const listenAddressHost = '0.0.0.0'
        const listenAddressPort = 8080
        const sampleAppDeploymentConstructProps : GeneralSampleAppDeploymentConstructProps = {
            cluster: props.cluster,
            namespaceConstruct: collectorNamespaceConstruct,
            sampleAppLabel: sampleAppLabel,
            sampleAppImageURI: props.sampleAppImageURI,
            sampleAppMode: props.sampleAppMode,
            listenAddressHost: listenAddressHost,
            listenAddressPort: listenAddressPort,
            region: props.region
        }
        if (deployServices){
            sampleAppDeploymentConstructProps.grpcServiceName = grpcServiceName
            sampleAppDeploymentConstructProps.grpcPort = grpcPort
            sampleAppDeploymentConstructProps.udpServiceName = udpServiceName
            sampleAppDeploymentConstructProps.udpPort = udpPort
            sampleAppDeploymentConstructProps.tcpServiceName = tcpServiceName
            sampleAppDeploymentConstructProps.httpPort = httpPort
        }
        new GeneralSampleAppDeploymentConstruct(this, 'general-sample-app-deployment-construct', sampleAppDeploymentConstructProps)

        // // general Collector deployment
        const generaCollectorDeploymentConstructProps : GeneralCollectorDeploymentConstructProps = {
            cluster: props.cluster,
            namespaceConstruct: collectorNamespaceConstruct,
            collectorConfig: props.collectorConfig,
            deployServices: deployServices
        }
        if (deployServices) {
            generaCollectorDeploymentConstructProps.grpcServiceName = grpcServiceName
            generaCollectorDeploymentConstructProps.grpcPort = grpcPort
            generaCollectorDeploymentConstructProps.udpServiceName = udpServiceName
            generaCollectorDeploymentConstructProps.udpPort= udpPort,
            generaCollectorDeploymentConstructProps.tcpServiceName = tcpServiceName
            generaCollectorDeploymentConstructProps.httpPort = httpPort
        }
        new GeneralCollectorDeploymentConstruct(this, 'general-collector-deployment-construct', generaCollectorDeploymentConstructProps)
    }
}

export interface TestCaseResourceDeploymentConstructProps {
    cluster: Cluster | FargateCluster
    sampleAppImageURI: string
    sampleAppMode: string
    region: string
    collectorConfig: string
}