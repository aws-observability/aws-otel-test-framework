import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';

export class NamespaceConstruct extends Construct {
    name: string
    namespace: Construct

    constructor(scope: Construct, id: string, props: NamespaceConstructProps){
        super(scope, id);
        this.name = props.name
        
        const namespaceManifest = {
            apiVersion: 'v1',
            kind: 'Namespace',
            metadata: { 
                name: this.name
            },
        }

        this.namespace = props.cluster.addManifest(props.name, namespaceManifest)
    }
}

export interface NamespaceConstructProps {
    cluster: Cluster | FargateCluster
    name: string
}