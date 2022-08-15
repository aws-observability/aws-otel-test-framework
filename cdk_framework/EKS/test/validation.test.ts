// import assert from 'assert';
// import { validateClustersConfig} from '../lib/utils/validate-cluster-config';


// const defaultSetUpTable = Object.entries({
//     'Bad kubernetes version 1.22': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     },
//                      version : 1.22
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'Regular working deployment': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: {
//                         ec2 : {
//                             ec2_instance : 'm5',
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'Too many fields in the cluster': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : 1.21, 
//                     random_field: 'random_value'
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'Missing ec2_instance field': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2: {
//                             node_size: 'xlarge'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'xlarge'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'Missing node_size field': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2: {
//                             ec2_instance: 'm5'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'Missing ec2 fields': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2: null
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'launch_type is null': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: null, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'Too many launch_types': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }, 
//                         fargate: null
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'Does not have clusters category': {
//         data: {
//             amdCluster: { 
//                 launch_type: { 
//                     ec2 : {
//                         ec2_instance : 'm5', 
//                         node_size: 'large'
//                     }
//                 }, 
//                 version : 1.21
//             }
//         },
//         expectedErr: Error
//     },
//     'Working fargate deployment': {
//         data: {
//             clusters: {
//                 fargateCluster: { 
//                     launch_type: { 
//                         fargate: null
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 fargateCluster: { 
//                     launch_type: { 
//                         fargate: null
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'Multiple deployment - fargate and ec2': {
//         data: {
//             clusters: {
//                 fargateCluster: { 
//                     launch_type: { 
//                         fargate: null
//                     }, 
//                     version : 1.21
//                 }, 
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 fargateCluster: { 
//                     launch_type: { 
//                         fargate: null
//                     }, 
//                     version : "1.21"
//                 }, 
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     'Multiple deployment - two ec2s': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'xlarge'
//                         }
//                     }, 
//                     version : 1.19
//                 }, 
//                 amdCluster2: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedOut: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'xlarge'
//                         }
//                     }, 
//                     version : "1.19"
//                 }, 
//                 amdCluster2: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : "1.21"
//                 }
//             }
//         }
//     },
//     't4g size is not good': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 't4g', 
//                             node_size: '4xlarge'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'm6g size is not good': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm6g', 
//                             node_size: '24xlarge'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'm5 size is not good': {
//         data: {
//             cluster: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'medium'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'ec2 instance invalid name': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'wrong_name', 
//                             node_size: 'large'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'launch_type is invalid': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     launch_type: { 
//                         wrong_type : {
//                             ec2_instance : 'm6g', 
//                             node_size: '24xlarge'
//                         }
//                     }, 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'version not provided': {
//         data: {
//             cluster: {
//                 amdCluster: { 
//                     launch_type: { 
//                         ec2 : {
//                             ec2_instance : 'm5', 
//                             node_size: 'medium'
//                         }
//                     }
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'launch_type not provided': {
//         data: {
//             clusters: {
//                 amdCluster: { 
//                     version : 1.21
//                 }
//             }
//         },
//         expectedErr: Error
//     },
//     'no clusters provided': {
//         data: {
//             clusters: null
//         },
//         expectedErr: Error
//     },
// })

// defaultSetUpTable.forEach(([name, fields]) => 
//     test(name, () => {
//         if(fields.expectedErr){
//             try{
//                 validateClustersConfig(fields.data)
//                 assert(false)
//             } catch(error){
//                 expect(error).toBeInstanceOf(Error);
//             }
//         } else {
//             validateClustersConfig(fields.data)
//             expect((fields.data)).toEqual(fields.expectedOut);
//         }
//     })
// )


