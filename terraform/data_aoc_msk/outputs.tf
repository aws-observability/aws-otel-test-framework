output "cluster_data" {
    description = "Cluster data for the MSK Cluster as return by aws_msk_cluster terraform data moduel + the topic name that the testcase should use"
    value = merge(local.cluster_data, {topic = local.topic})
}
