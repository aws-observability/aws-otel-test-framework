import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';

export class SampleAppServiceConstruct extends Construct {
    name: string
    sampleAppService: Construct

    constructor(scope: Construct, id: string, props: SampleAppServiceConstructProps){
        super(scope, id);
        
        const sampleAppServiceManifest = {
            apiVersion: 'v1',
            kind: 'Service',
            metadata: {
                name: `${props.sampleAppLabel}-service`,
                namespace: props.namespaceConstruct.name,
                labels: {
                    app: props.sampleAppLabel,
                    component: props.sampleAppLabel
                }
            },

            spec:{
                ports: [
                    {
                        name: 'metrics',
                        port: props.listenAddressPort
                    }
                ],
                selector: {
                    app: props.sampleAppLabel
                },
                type: 'LoadBalancer'
            }
        }

        this.sampleAppService = props.cluster.addManifest('sample-app-service', sampleAppServiceManifest)
        this.sampleAppService.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface SampleAppServiceConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    sampleAppLabel: string
    listenAddressPort: number
}