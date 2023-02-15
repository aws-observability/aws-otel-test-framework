// Prefix used in the cluster name
// Should use the same prefix of the clusters created in the cdk_infra module.
variable "cluster_name_prefix" {
    default = "AOCMSKCluster"
}

// The Kafka Cluster version
variable "cluster_version" {
    default = "2.8.1"
}

// Used to make sure that we don't have topic collisions. We cannot generate infinite amount of topics
// but we also don't want tests to collide.
variable "dedup_topic" {
    default = ""
}

// Testcase name as passed to each main module (ec2, ecs or eks). E.g.: -var="testcase=../testcases/statsd"
variable "testcase" {
    default = ""
}
