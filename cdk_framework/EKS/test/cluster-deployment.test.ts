import * as cdk from 'aws-cdk-lib';
import { Template } from 'aws-cdk-lib/assertions';
import { readFileSync} from 'fs';
import { ClusterStack } from '../lib/stacks/cluster-stack';
import { deployClusters } from '../lib/cluster-deployment';

const yaml = require('js-yaml')

test('ClusterTest', () => {
    const app = new cdk.App();

    const route = __dirname + '/test_config/test_clusters.yml';
    const raw = readFileSync(route)
    const data = yaml.load(raw)


    let clusterMap = new Map<string, ClusterStack>()
    const versionMap = new Map<string, string>()

    clusterMap = deployClusters(app, data)

    for(const [key, value] of Object.entries(data['clusters'])){
      const val = Object(value)
      versionMap.set(key, String(val['version']))

  
  }

    for(const [key, st] of clusterMap){
        const template = Template.fromStack(st);
        const KubernetesVersion = versionMap.get(key)

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
