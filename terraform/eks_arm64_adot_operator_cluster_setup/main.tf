# ------------------------------------------------------------------------
# Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License").
# You may not use this file except in compliance with the License.
# A copy of the License is located at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# or in the "license" file accompanying this file. This file is distributed
# on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
# express or implied. See the License for the specific language governing
# permissions and limitations under the License.
# -------------------------------------------------------------------------

terraform {
  required_version = "=1.1.7"
  required_providers {
    kubectl = {
      source  = "gavinbunney/kubectl"
      version = ">= 1.7.0"
    }
  }
  backend "s3" {
    bucket  = "aws-otel-test-terraform-state"
    key     = "eks_arm64_adot_operator_cluster/terraform.tfstate"
    region  = "us-west-2"
    encrypt = true
  }
}

module "common" {
  source = "../common"
}

provider "aws" {
  region = var.region
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "3.11.0"

  name = "${var.eks_cluster_name}-vpc"
  cidr = "10.0.0.0/16"

  azs             = ["${var.region}a", "${var.region}b", "${var.region}c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = true
  single_nat_gateway = true
}

module "eks" {
  source  = "terraform-aws-modules/eks/aws"
  version = "17.24.0"

  cluster_version = "1.21"
  cluster_name    = var.eks_cluster_name
  vpc_id          = module.vpc.vpc_id
  subnets         = module.vpc.public_subnets
  enable_irsa     = true

  cluster_endpoint_public_access = true

  cluster_enabled_log_types = ["api", "audit", "authenticator", "controllerManager", "scheduler"]

  node_groups = {
    arm64_node_group = {
      name         = "arm64-node-group"
      ami_type     = "AL2_ARM_64"
      min_size     = 3
      max_size     = 5
      desired_size = 5

      instance_types = ["m6g.medium"]
      capacity_type  = "SPOT"

      iam_role_additional_policies = [
        "arn:aws:iam::aws:policy/AmazonPrometheusRemoteWriteAccess",
        "arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess",
        "arn:aws:iam::aws:policy/CloudWatchAgentAdminPolicy",
        "arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess",
        "arn:aws:iam::aws:policy/AWSAppMeshEnvoyAccess",
        "arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy",
        "arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly",
        "arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy"
      ]
    }
  }
}

data "aws_eks_cluster" "eks" {
  name = module.eks.cluster_id
}

data "aws_eks_cluster_auth" "eks" {
  name = module.eks.cluster_id
}

provider "kubernetes" {
  host                   = data.aws_eks_cluster.eks.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.eks.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.eks.token
}

provider "kubectl" {
  // Note: copy from eks module. Please avoid use shorted-lived tokens when running locally.
  // For more information: https://registry.terraform.io/providers/hashicorp/kubernetes/latest/docs#exec-plugins
  host                   = data.aws_eks_cluster.eks.endpoint
  cluster_ca_certificate = base64decode(data.aws_eks_cluster.eks.certificate_authority[0].data)
  token                  = data.aws_eks_cluster_auth.eks.token
  load_config_file       = false
}

provider "helm" {
  kubernetes {
    host                   = data.aws_eks_cluster.eks.endpoint
    cluster_ca_certificate = base64decode(data.aws_eks_cluster.eks.certificate_authority[0].data)
    token                  = data.aws_eks_cluster_auth.eks.token
  }
}

resource "helm_release" "adot-operator-cert-manager" {
  name      = "cert-manager"
  namespace = "cert-manager"

  repository = "https://charts.jetstack.io"
  chart      = "cert-manager"
  version    = "v1.8.1"

  create_namespace = true

  set {
    name  = "installCRDs"
    value = "true"
  }

  provisioner "local-exec" {
    # We need to set up a 20 seconds sleep to avoid the certificate signed by unknown authority issue which appears occasionally.
    command = <<-EOT
      kubectl wait --kubeconfig=${module.eks.kubeconfig_filename} --timeout=5m --for=condition=available deployment cert-manager -n cert-manager
      kubectl wait --kubeconfig=${module.eks.kubeconfig_filename} --timeout=5m --for=condition=available deployment cert-manager-webhook -n cert-manager
      sleep 20s
    EOT
  }
}
