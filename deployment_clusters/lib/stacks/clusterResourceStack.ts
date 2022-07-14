

import { Stack, StackProps, aws_eks as eks, aws_ec2 as ec2} from "aws-cdk-lib";
import { EnvironmentFile } from "aws-cdk-lib/aws-ecs";
import { Cluster } from "aws-cdk-lib/aws-eks";
import { EksCall } from "aws-cdk-lib/aws-stepfunctions-tasks";
import { Construct } from "constructs";
import { ClusterManagementStack } from "./cluster-management-stack";
import { ClusterStack } from "./cluster-stack";

interface ClusterResourceProps extends StackProps{
    clusterStack: ClusterStack
}

export class ClusterResourceStack extends Stack{
    constructor(scope: Construct, id: string, props: ClusterResourceProps ){
        if (props.clusterStack === undefined){
            return
        }
        super(scope,id);
        const cms = props.clusterStack;
        // const cs = cms.getCluster("armCluster");
        const cs = cms.cluster;
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


// interface ClusterResourceProps{
//     cluster: eks.ICluster
// }

// export class ClusterResourceStack extends Stack{
//     constructor(scope: Construct, id: string, props: ClusterResourceProps ){
//         super(scope,id);
//         const cs = props.cluster;
//         // const cs = cms.getCluster("armCluster");
//         if(cs != null){
//             const mani = cs.addManifest('test-resource', {
//                 apiVersion: 'v1',
//                 kind: 'Namespace',
//                 metadata: { name: 'test-namespace'},
//             })
//         }
//         // lets just see if this works. We want to deploy this separately
        
//     }
// }