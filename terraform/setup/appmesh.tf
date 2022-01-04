# ------------------------------------------------------------------------
# Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

resource "aws_iam_policy" "appmesh_k8s_iam_policy" {

  name = module.common.appmesh_k8s_iam_policy
  path = "/"

  # Terraform's jsonencode function converts a
  # Terraform expression result to valid JSON syntax.
  policy = jsonencode({
    Version = "2012-10-17",
    Statement = [
      {
        Effect = "Allow",
        Action = [
          "appmesh:ListVirtualRouters",
          "appmesh:ListVirtualServices",
          "appmesh:ListRoutes",
          "appmesh:ListGatewayRoutes",
          "appmesh:ListMeshes",
          "appmesh:ListVirtualNodes",
          "appmesh:ListVirtualGateways",
          "appmesh:DescribeMesh",
          "appmesh:DescribeVirtualRouter",
          "appmesh:DescribeRoute",
          "appmesh:DescribeVirtualNode",
          "appmesh:DescribeVirtualGateway",
          "appmesh:DescribeGatewayRoute",
          "appmesh:DescribeVirtualService",
          "appmesh:CreateMesh",
          "appmesh:CreateVirtualRouter",
          "appmesh:CreateVirtualGateway",
          "appmesh:CreateVirtualService",
          "appmesh:CreateGatewayRoute",
          "appmesh:CreateRoute",
          "appmesh:CreateVirtualNode",
          "appmesh:UpdateMesh",
          "appmesh:UpdateRoute",
          "appmesh:UpdateVirtualGateway",
          "appmesh:UpdateVirtualRouter",
          "appmesh:UpdateGatewayRoute",
          "appmesh:UpdateVirtualService",
          "appmesh:UpdateVirtualNode",
          "appmesh:DeleteMesh",
          "appmesh:DeleteRoute",
          "appmesh:DeleteVirtualRouter",
          "appmesh:DeleteGatewayRoute",
          "appmesh:DeleteVirtualService",
          "appmesh:DeleteVirtualNode",
          "appmesh:DeleteVirtualGateway"
        ],
        Resource = "*"
      },
      {
        Effect = "Allow",
        Action = [
          "iam:CreateServiceLinkedRole"
        ],
        Resource = "arn:aws:iam::${module.common.aws_account_id}:role/aws-service-role/appmesh.amazonaws.com/AWSServiceRoleForAppMesh",
        Condition = {
          StringLike = {
            "iam:AWSServiceName" = [
              "appmesh.amazonaws.com"
            ]
          }
        }
      },
      {
        Effect = "Allow",
        Action = [
          "acm:ListCertificates",
          "acm:DescribeCertificate",
          "acm-pca:DescribeCertificateAuthority",
          "acm-pca:ListCertificateAuthorities"
        ],
        Resource = "*"
      },
      {
        Effect = "Allow",
        Action = [
          "servicediscovery:CreateService",
          "servicediscovery:DeleteService",
          "servicediscovery:GetService",
          "servicediscovery:GetInstance",
          "servicediscovery:RegisterInstance",
          "servicediscovery:DeregisterInstance",
          "servicediscovery:ListInstances",
          "servicediscovery:ListNamespaces",
          "servicediscovery:ListServices",
          "servicediscovery:GetInstancesHealthStatus",
          "servicediscovery:UpdateInstanceCustomHealthStatus",
          "servicediscovery:GetOperation",
          "route53:GetHealthCheck",
          "route53:CreateHealthCheck",
          "route53:UpdateHealthCheck",
          "route53:ChangeResourceRecordSets",
          "route53:DeleteHealthCheck"
        ],
        Resource = "*"
    }]
  })
}
