

import { Stack } from "aws-cdk-lib";
import { EnvironmentFile } from "aws-cdk-lib/aws-ecs";
import { Cluster } from "aws-cdk-lib/aws-eks";
import { EksCall } from "aws-cdk-lib/aws-stepfunctions-tasks";
import { Construct } from "constructs";
import { ClusterManagementStack } from "./cluster-management-stack";

interface ClusterResourceProps{
    clusterManagementStack: ClusterManagementStack
}

export class ClusterResourceStack extends Stack{
    constructor(scope: Construct, id: string, props: ClusterResourceProps ){
        super(scope,id);
        const cms = props.clusterManagementStack;
        const cs = cms.getCluster("armCluster");
        if(cs != null){
            const mani = cs.addManifest('test-resource', {
                apiVersion: 'v1',
                kind: 'Namespace',
                metadata: { name: 'test-namespace'},
            })
        }
        // lets just see if this works. We want to deploy this separately
        
    }
}