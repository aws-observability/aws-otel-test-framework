import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { PushModeSampleAppDeploymentConstruct } from './push-mode-sample-app-construct';
import { PullModeSampleAppDeploymentConstruct } from './pull-mode-sample-app-construct';
import { NamespaceConstruct } from './namespace-construct';
import { SampleAppServiceConstruct } from './sample-app-service-construct';


export class GeneralSampleAppDeploymentConstruct extends Construct {

    constructor(scope: Construct, id: string, props: GeneralSampleAppDeploymentConstructProps){
         super(scope, id);

        new SampleAppServiceConstruct(this, 'sample-app-service-construct', {
            cluster: props.cluster,
            namespaceConstruct: props.namespaceConstruct,
            sampleAppLabel: props.sampleAppLabel,
            listenAddressPort: props.listenAddressPort
        })

        if (props.sampleAppMode === 'push'){
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
            new PushModeSampleAppDeploymentConstruct(this, 'push-mode-sample-app-construct', {
                cluster: props.cluster,
                namespaceConstruct: props.namespaceConstruct,
                sampleAppLabel: props.sampleAppLabel,
                sampleAppImageURI: props.sampleAppImageURI,
                grpcServiceName: props.grpcServiceName,
                grpcPort: props.grpcPort,
                udpServiceName: props.udpServiceName,
                udpPort: props.udpPort,
                tcpServiceName: props.tcpServiceName,
                httpPort: props.httpPort,
                listenAddressHost: props.listenAddressHost,
                listenAddressPort: props.listenAddressPort,
                region: props.region
            })
        }
        else if (props.sampleAppMode === 'pull'){
            new PullModeSampleAppDeploymentConstruct(this, 'pull-mode-sample-app-construct', {
                cluster: props.cluster,
                namespaceConstruct: props.namespaceConstruct,
                sampleAppLabel: props.sampleAppLabel,
                sampleAppImageURI: props.sampleAppImageURI,
                listenAddressHost: props.listenAddressHost,
                listenAddressPort: props.listenAddressPort,
                region: props.region
            })
        }
    }
}

export interface GeneralSampleAppDeploymentConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    sampleAppLabel: string
    sampleAppImageURI: string
    sampleAppMode: string
    listenAddressHost: string
    listenAddressPort: number
    region: string
    grpcServiceName?: string
    grpcPort?: number
    udpServiceName?: string
    udpPort?: number
    tcpServiceName?: string
    httpPort?: number
}