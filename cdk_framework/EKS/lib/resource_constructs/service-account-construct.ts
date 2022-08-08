import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';

export class ServiceAccountConstruct extends Construct {
    name: string
    serviceAccount: Construct

    constructor(scope: Construct, id: string, props: ServiceAccountConstructProps){
        super(scope, id);
        this.name = props.name

        //There's a service account constuct in CDK, should we use that?
        const serviceAccountManifest = {
            apiVersion: "v1",
            kind: "ServiceAccount",
            metadata: {
                name: this.name,
                namespace: props.namespaceConstruct.name
            },
            
            automountServiceAccountToken: true
        }

        this.serviceAccount = props.cluster.addManifest(props.name, serviceAccountManifest)
        this.serviceAccount.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface ServiceAccountConstructProps {
    cluster: Cluster | FargateCluster
    name: string
    namespaceConstruct: NamespaceConstruct
}