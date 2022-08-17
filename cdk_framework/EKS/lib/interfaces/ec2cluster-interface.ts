import {ClusterInterface} from "./cluster-interface"

export interface ec2ClusterInterface extends ClusterInterface{
    instance_type: string
}
