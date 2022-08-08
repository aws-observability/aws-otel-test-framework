import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { CollectorConfigMapConstruct } from './collector-config-map-construct';
import { NamespaceConstruct } from './namespace-construct';
import { ServiceAccountConstruct } from './service-account-construct';

export class CollectorDeploymentConstruct extends Construct {
    collectorDeployment: Construct

    constructor(scope: Construct, id: string, props: CollectorDeploymentConstructProps) {
        super(scope, id);

        const collectorConfigMountPath = '/collector'
        const collectorDeploymentManifest = {
            apiVersion: 'apps/v1',
            kind: 'Deployment',

            metadata: {
                name: 'collector',
                namespace: props.namespaceConstruct.name,
                labels: {
                    app: 'collector'
                }
            },
            
            spec: {
                replicas: 1,
            
                selector: {
                    matchLabels: {
                        app: props.collectorAppLabel
                    }
                },
            
                template: {
                    metadata: {
                        labels: {
                            app: props.collectorAppLabel
                        }
                    },
            
                    spec: {
                        serviceAccountName: props.serviceAccountConstruct.name,
                        automountServiceAccountToken: true,
                        
                        volumes: [
                            {
                                name: props.collectorConfigMapConstruct.name,
                                configMap: {
                                    name: props.collectorConfigMapConstruct.name
                                }
                            },

                            // {
                            //     name: props.mockedServerCertConstruct.name,
                            //     configMap: {
                            //         // name: kubernetes_config_map.mocked_server_cert.0.metadata[0].name
                            //         name: props.mockedServerCertConstruct.name
                            //     }
                            // }
                        ],

                        containers: [
                            // {
                            //     name: 'mocked-server',
                            //     image: local.mocked_server_image,
                            //     image: "${data.aws_ecr_repository.mocked_servers.repository_url}:${var.mocked_server}-latest",
                            //     imagePullPolicy: 'Always',
                        
                            //     readinessProbe: {
                            //         httpGet: {
                            //             path: '/',
                            //             port: 8080
                            //         },
                            //         initialDelaySeconds: 10,
                            //         periodSeconds: 5
                            //     }
                            // },
                            {
                                name: 'collector',
                                image: 'public.ecr.aws/aws-otel-test/adot-collector-integration-test:latest',
                                imagePullPolicy: 'Always',
                                args: [
                                `--config=${collectorConfigMountPath}/${props.collectorConfigMapConstruct.collectorConfigPath}`],
                        
                                resources: {
                                    limits: {
                                        cpu: '0.2',
                                        memory: '256Mi'
                                    }
                                },

                                volumeMounts: [
                                    {
                                        mountPath: collectorConfigMountPath,
                                        name: props.collectorConfigMapConstruct.name
                                    }
                                    // {
                                    //     mountPath: '/etc/pki/tls/certs',
                                    //     name: props.mockedServerCertConstruct.name
                                    // }
                                ]
                            }
                        ]
                    }
                }
            },

            //dependsOn: [aws_eks_fargate_profile.test_profile]
        }
        
        this.collectorDeployment = props.cluster.addManifest('collector-deployment', collectorDeploymentManifest)
        this.collectorDeployment.node.addDependency(props.namespaceConstruct.namespace)
        this.collectorDeployment.node.addDependency(props.serviceAccountConstruct.serviceAccount)
        this.collectorDeployment.node.addDependency(props.collectorConfigMapConstruct.collectorConfigMap)
    }
}

export interface CollectorDeploymentConstructProps {
    cluster: Cluster | FargateCluster
    namespaceConstruct: NamespaceConstruct
    collectorAppLabel: string
    serviceAccountConstruct: ServiceAccountConstruct
    collectorConfigMapConstruct: CollectorConfigMapConstruct
}