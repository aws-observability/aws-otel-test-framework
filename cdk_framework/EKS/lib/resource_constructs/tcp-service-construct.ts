import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';

export class TCPServiceConstruct extends Construct {
    tcpService: Construct

    constructor(scope: Construct, id: string, props: TCPConstructProps){
        super(scope, id);
        
        const tcpServiceManifest = {
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
                        port: props.httpPort,
                        targetPort: props.httpPort,
                    }
                ]
            }
        }

        this.tcpService = props.cluster.addManifest(props.name, tcpServiceManifest)
        this.tcpService.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface TCPConstructProps {
    cluster: Cluster | FargateCluster
    name: string
    namespaceConstruct: NamespaceConstruct
    appLabel: string
    httpPort: number
}