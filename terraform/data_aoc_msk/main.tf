// Module used to enforce conventions of Cluster names and topic names.
// This module will extract data from MSK clusters based on the name conventions
// followed during the creation of the cluster in the cdk_infra package.
// It will also generate the name of the topic that should be used in a test case.
//
// The reason why we need to generate the name of the topic is because topics
// are a finite resource in Kafka and we must reuse them across tests while still
// avoid collision between tests.
// Topic names will have the following format: <branch>-<testcase name>-<dedupstring>
locals {
  // E.g.: "AOCMSKCluster2-8-1"
  cluster_name = "${var.cluster_name_prefix}${replace(var.cluster_version, ".", "-")}"
}

data "aws_msk_cluster" "aoc_cluster" {
  cluster_name = local.cluster_name

  count = var.cluster_version == "" ? 0 : 1
}

data "external" "branch" {
  // The output of the command has to be a json.
  program = ["/bin/bash", "-c", "echo \"{\\\"branch\\\": \\\"`git rev-parse --abbrev-ref HEAD`\\\"}\""]
}

locals {
  topic_pieces   = split("/", var.testcase)
  topic_testcase = element(local.topic_pieces, length(local.topic_pieces) - 1)
  // Branch name with "/" replaced by "-"
  branch = replace(trim(data.external.branch.result["branch"], " "), "/[\\/]/", "-")

  topic        = "${local.branch}-${local.topic_testcase}-${var.dedup_topic}"
  cluster_data = var.cluster_version == "" ? tomap({}) : data.aws_msk_cluster.aoc_cluster[0]
}
