import { Construct } from 'constructs';
import { Cluster, FargateCluster } from 'aws-cdk-lib/aws-eks';
import { NamespaceConstruct } from './namespace-construct';


export class PushModeSampleAppDeploymentConstruct extends Construct {
   pushModeSampleAppDeployment: Construct

   constructor(scope: Construct, id: string, props: PushModeSampleAppDeploymentConstructProps) {
        super(scope, id);

        const pushModeSampleAppDeploymentManifest = {
            apiVersion: 'apps/v1',
            kind: 'Deployment',
         
            metadata: {
                name: 'push-mode-sample-app',
                namespace: props.namespaceConstruct.name,
                labels: {
                    app: 'push-mode-sample-app'
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
                                //command: length(local.eks_pod_config['command']) != 0 ? local.eks_pod_config['command'] : null,
                                command: null,
                                //args: length(local.eks_pod_config['args']) != 0 ? local.eks_pod_config['args'] : null,
                                args: null,

                                env: [
                                    {
                                        name: 'OTEL_EXPORTER_OTLP_ENDPOINT',
                                        //value: var.is_adot_operator ? 'http://aoc-collector:${var.aoc_service.grpc_port}' : 'http://${kubernetes_service.aoc_grpc_service[0].metadata[0].name}:${var.aoc_service.grpc_port}'
                                        value : `http://${props.grpcServiceName}:${props.grpcPort}`
                                    },
                                    {
                                        name: 'COLLECTOR_UDP_ADDRESS',
                                        value: `${props.udpServiceName}:${props.udpPort}`
                                    },
                                    {
                                        name: 'AWS_XRAY_DAEMON_ADDRESS',
                                        value: `${props.udpServiceName}:${props.udpPort}`
                                    },
                                    {
                                        name: 'AWS_REGION',
                                        value: props.region
                                    },
                                    {
                                        name: 'INSTANCE_ID',
                                        // value: var.testing_id
                                        value: '1'
                                    },
                                    {
                                        name: 'OTEL_RESOURCE_ATTRIBUTES',
                                        // value: 'service.namespace=${var.sample_app.metric_namespace},service.name=${var.aoc_service.name}'
                                        value: 'service.namespace=aws-otel,service.name=aws-otel-integ-test'
                                    },
                                    {
                                        name: 'LISTEN_ADDRESS',
                                        value: `${props.listenAddressHost}:${props.listenAddressPort}`
                                    },
                                    {
                                        name: 'JAEGER_RECEIVER_ENDPOINT',
                                        value: `${props.tcpServiceName}:${props.httpPort}`
                                    },
                                    {
                                        name: 'ZIPKIN_RECEIVER_ENDPOINT',
                                        value: `${props.tcpServiceName}:${props.httpPort}`
                                    },
                                    {
                                        name: 'OTEL_METRICS_EXPORTER',
                                        value: 'otlp'
                                    }
                                ] ,

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

        this.pushModeSampleAppDeployment = props.cluster.addManifest('push-mode-sample-app-deployment', pushModeSampleAppDeploymentManifest)
        this.pushModeSampleAppDeployment.node.addDependency(props.namespaceConstruct.namespace)
    }
}

export interface PushModeSampleAppDeploymentConstructProps {
      cluster: Cluster | FargateCluster
      namespaceConstruct: NamespaceConstruct
      sampleAppLabel: string
      sampleAppImageURI: string
      listenAddressHost: string
      listenAddressPort: number
      region: string
      grpcServiceName: string
      grpcPort: number
      udpServiceName: string
      udpPort: number
      tcpServiceName: string
      httpPort: number
}