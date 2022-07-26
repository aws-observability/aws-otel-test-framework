import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { readFileSync, writeFileSync } from 'fs';
import { validateClustersConfig } from '../lib/utils/validate';
import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from 'aws-cdk-lib';
import { ClusterStack } from '../lib/stacks/cluster-stack';
import { VPCStack } from '../lib/utils/vpc-stack';
const yaml = require('js-yaml')

test('ClusterTest', () => {
    var app = new cdk.App();
    // WHEN
    const route = process.env.CDK_CONFIG_PATH ||  __dirname + '/config/clusters.yml';
    const raw = readFileSync(route)
    const data = yaml.load(raw)

    var clusterMap = new Map<string, ClusterStack>()
    var versionMap = new Map<string, string>()

    const vs = new VPCStack(app, "EKSVpc", {
        env: {
          region: 'us-west-2'
        }
      })

    validateClustersConfig(data)
    for(const [key, value] of Object.entries(data['clusters'])){
        const val = Object(value)
        const versionKubernetes = eks.KubernetesVersion.of(String(val['version']));
        const newStack = new ClusterStack(app, key + "Stack", {
            launchType: String(val['launch_type']),
            name: key,
            vpc: vs.vpc,
            version: versionKubernetes,
            cpu: String(val["cpu_architecture"]),
            nodeSize: String(val["node_size"]),
            env: {
            region: 'us-west-2'
            },
        })

        clusterMap.set(key, newStack)
        versionMap.set(key, String(val['version']))

    
    }

    for(const [key, st] of clusterMap){
        var template = Template.fromStack(st);
        var KubernetesVersion = versionMap.get(key)

        template.hasResourceProperties("Custom::AWSCDK-EKS-Cluster", {
            Config: {
                name: key,
                version: KubernetesVersion,
                logging: {
                  clusterLogging: [
                    {
                      enabled: true,
                      types: [
                        "api",
                        "audit",
                        "authenticator",
                        "controllerManager",
                        "scheduler"
                      ]
                    }
                  ]
                }
            }
        })
    }

});
