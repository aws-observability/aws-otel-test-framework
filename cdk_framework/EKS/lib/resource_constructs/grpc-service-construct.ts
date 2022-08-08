import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';

export class GRPCServiceConstruct extends Construct {
    grpcService: Construct

    constructor(scope: Construct, id: string, props: GRPCConstructProps){
        super(scope, id);
        
        const grpcServiceManifest = {
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
                        port: props.grpcPort,
                        targetPort: props.grpcPort,
                        protocol: 'TCP'
                    }
                ]
            }
        }

        this.grpcService = props.cluster.addManifest(props.name, grpcServiceManifest)
        this.grpcService.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface GRPCConstructProps {
    cluster: Cluster | FargateCluster
    name: string
    namespaceConstruct: NamespaceConstruct
    appLabel: string
    grpcPort: number
}