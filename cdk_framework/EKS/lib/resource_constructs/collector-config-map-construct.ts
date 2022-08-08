import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';


export class CollectorConfigMapConstruct extends Construct {
        name: string
        collectorConfigPath: string
        collectorConfigMap: Construct

        constructor(scope: Construct, id: string, props: CollectorConfigMapConstructProps) {
            super(scope, id);
            this.name = 'collector-config-map'
            
            const collectorConfigMapManifest = {
                apiVersion: 'v1',
                kind: 'ConfigMap',

                metadata: {
                    name: this.name,
                    namespace: props.namespaceConstruct.name
                },
                
                data: {
                    'collector-config.yml': JSON.stringify(props.collectorConfig)
                },

                // depends_on: [aws_eks_fargate_profile.test_profile]
            }

            this.collectorConfigPath = Object.keys(collectorConfigMapManifest['data'])[0]
            this.collectorConfigMap = props.cluster.addManifest(this.name, collectorConfigMapManifest)
            this.collectorConfigMap.node.addDependency(props.namespaceConstruct.namespace)
        }
}

export interface CollectorConfigMapConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    collectorConfig: any
}