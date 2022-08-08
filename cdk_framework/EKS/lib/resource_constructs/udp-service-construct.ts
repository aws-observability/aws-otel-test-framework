import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';

export class UDPServiceConstruct extends Construct {
    udpService: Construct

    constructor(scope: Construct, id: string, props: UDPConstructProps){
        super(scope, id);
        
        const udpServiceManifest = {
            apiVersion: 'v1',
            kind: 'Service',

            metadata: {
                name: props.name,
                namespace: props.namespaceConstruct.name
            },
            spec: {
                selector: {
                    app: props.appLabel
                },
            
                ports: [
                    {
                        port: props.udpPort,
                        targetPort: props.udpPort,
                        protocol: 'UDP'
                    }
                ]
            }
        }

        this.udpService = props.cluster.addManifest(props.name, udpServiceManifest)
        this.udpService.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface UDPConstructProps {
    cluster: Cluster | FargateCluster
    name: string
    namespaceConstruct: NamespaceConstruct
    appLabel: string
    udpPort: number
}