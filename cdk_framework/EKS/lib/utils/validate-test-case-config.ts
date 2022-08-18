import { TestCaseConfigInterface } from '../interfaces/test-case-config-interface';
import { ClusterStack } from '../stacks/cluster-stack';

export function validateTestCaseConfig(testCaseConfig: TestCaseConfigInterface, clusterStackMap: Map <string, ClusterStack>){
    if (testCaseConfig['cluster_name'] == undefined) {
        throw Error('No cluster_name specified')
    }
    if (testCaseConfig['sample_app_image_uri'] == undefined) {
        throw Error('No sample_app_image_uri specified')
    }
    if (testCaseConfig['sample_app_mode'] == undefined) {
        throw Error('No sample_app_mode specified')
    }
    if (testCaseConfig['collector_config'] == undefined) {
        throw Error('No collector_config specified')
    }
    if (clusterStackMap.get(testCaseConfig['cluster_name']) == undefined) {
        throw Error(`${testCaseConfig['cluster_name']} does not reference an existing cluster`)
    }
    if (testCaseConfig['sample_app_mode'] !== 'push' && testCaseConfig['sample_app_mode'] !== 'push') {
        throw Error(`sample_app_mode must have value "push" or "pull", ${testCaseConfig['sample_app_mode']} is invalid`)
    }
}