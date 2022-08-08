import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ClusterStack } from './stacks/cluster-stack';
import { validateTestcaseConfig } from './utils/validate-test-case-config';
import { readFileSync } from 'fs';
const yaml = require('js-yaml')
import { TestCaseResourceDeploymentConstruct } from './resource_constructs/test-case-resource-deployment-construct';


export function deployResources(app: cdk.App, clusterStackMap: Map <string, ClusterStack>) {
    const region = process.env.REGION || 'us-west-2'
    
    // load the file
    const testcaseConfigRoute = process.env.TESTCASE_CONFIG_PATH
    // if no testcase config path is provided, throw error
    if (testcaseConfigRoute == undefined){
        throw new Error ('No path provided for testcase configuration')
    }
    // if testcase config path doesn't route to a yaml file, throw error
    if (!/(.yml|.yaml)$/.test(testcaseConfigRoute)){
        throw new Error ('Path for testcase configuration must be to a yaml file')
    }

    // load the data from the file
    const raw = readFileSync(testcaseConfigRoute, {encoding: 'utf-8'})
    const data = yaml.load(raw)
    validateTestcaseConfig(data, clusterStackMap)
    const testcaseConfig = data['test_case']

    const clusterName = testcaseConfig['cluster_name']
    const clusterStack = clusterStackMap.get(clusterName)
    if (clusterStack == undefined) {
        throw Error('Cluster name "' + clusterName + '" does not reference an existing cluster')
    }
    const cluster = clusterStack.cluster
    const sampleAppImageURI = testcaseConfig['sample_app_image_uri']
    const sampleAppMode = testcaseConfig['sample_app_mode']
    const collectorConfig = testcaseConfig['collector_config']

    new TestCaseResourceDeploymentConstruct(clusterStack, 'test-case-resource-deployment-construct', {
        cluster: cluster,
        sampleAppImageURI: sampleAppImageURI,
        sampleAppMode: sampleAppMode,
        region: region,
        collectorConfig: collectorConfig
    })
}