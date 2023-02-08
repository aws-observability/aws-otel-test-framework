// Module used to enforce conventions of Cluster names and topic names.

locals {
    // E.g.: "AOCMSKCluster2-8-1"
    cluster_name = "${var.cluster_name_prefix}${replace(var.cluster_version, ".", "-")}"
}

data "aws_msk_cluster" "aoc_cluster" {
  cluster_name = local.cluster_name

  count = var.cluster_version == "" ? 0 : 1
}

locals {
    topic_pieces = split("/", var.testcase)
    // Topic is dependent on the testcase and we also provide a dedup string in case multiple testscases are run in palallel.
    topic = "${var.dedup_topic}${element(local.topic_pieces, length(local.topic_pieces) - 1)}"
    cluster_data = var.cluster_version == "" ? tomap({}) : data.aws_msk_cluster.aoc_cluster[0]
}
