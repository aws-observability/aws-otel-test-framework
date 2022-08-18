import 'source-map-support/register';
import { ClusterStack } from './stacks/cluster-stack';
import { TestCaseConfigInterface } from './interfaces/test-case-config-interface';
import { validateTestCaseConfig } from './utils/validate-test-case-config';
import { readFileSync } from 'fs';
const yaml = require('js-yaml')
import { TestCaseResourceDeploymentConstruct } from './resource_constructs/general_constructs/test-case-resource-deployment-construct';


export function deployResources(clusterStackMap: Map <string, ClusterStack>) {
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
    if (!data['test_case']) {
        throw new Error('No root test_case field in the yaml file')
    }
    const testCaseConfig = data['test_case'] as TestCaseConfigInterface

    // validate the configuration
    validateTestCaseConfig(testCaseConfig, clusterStackMap)

    const clusterStack = clusterStackMap.get(testCaseConfig['cluster_name'])
    // This was already checked in the validation function but needs to be checked again
    // to avoid compilation errors
    if (clusterStack == undefined) {
        throw Error('Cluster name "' + testCaseConfig['cluster_name'] + '" does not reference an existing cluster')
    }

    new TestCaseResourceDeploymentConstruct(clusterStack, 'test-case-resource-deployment-construct', {
        cluster: clusterStack.cluster,
        sampleAppImageURI: testCaseConfig['sample_app_image_uri'],
        sampleAppMode: testCaseConfig['sample_app_mode'],
        region: region,
        collectorConfig: testCaseConfig['collector_config']
    })
}