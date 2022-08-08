import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';


export class PullModeSampleAppDeploymentConstruct extends Construct {
    pullModeSampleAppDeployment: Construct

    constructor(scope: Construct, id: string, props: PullModeSampleAppDeploymentConstructProps) {
        super(scope, id);
        const pullModeAppManifest = {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
            
            // maybe change name to 'pull-mode-sample-app'?
            metadata: {
                name: 'pull-mode-sample-app',
                namespace: props.namespaceConstruct.name,
                labels: {
                    name: 'pull-mode-sample-app',
                }
            },

            spec: {
                replicas: 1,

                selector: {
                    matchLabels: {
                        app: props.sampleAppLabel
                    }
                },

                template: {
                    metadata: {
                        labels: {
                            app: props.sampleAppLabel
                        }
                    },

                    spec: {
                        containers: [
                            {
                                name: 'sample-app',
                                image: props.sampleAppImageURI,
                                imagePullPolicy: 'Always',
                                command: null,
                                args: null,
                                env: [
                                    {
                                        name: 'AWS_REGION',
                                        value: props.region
                                    },
                                    {
                                        name: 'INSTANCE_ID',
                                        value: '1'
                                    },
                                    {
                                        name: 'LISTEN_ADDRESS',
                                        value: `${props.listenAddressHost}:${props.listenAddressPort}`
                                    }
                                ],

                                resources: {
                                    limits: {
                                        cpu: '0.2',
                                        memory: '256Mi'
                                    }
                                },

                                readinessProbe: {
                                    httpGet: {
                                        path: '/',
                                        port: props.listenAddressPort
                                    },
                                    initialDelaySeconds: 10,
                                    periodSeconds: 5
                                }
                            }
                        ]
                    }
                }
            }
        }
        
        this.pullModeSampleAppDeployment = props.cluster.addManifest('pull-mode-sample-app', pullModeAppManifest)
        this.pullModeSampleAppDeployment.node.addDependency(props.namespaceConstruct.namespace)
   }
}

export interface PullModeSampleAppDeploymentConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    sampleAppLabel: string
    sampleAppImageURI: string
    listenAddressHost: string
    listenAddressPort: number
    region: string
}