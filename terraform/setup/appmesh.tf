data "external" "appmesh_k8s_iam_policy_exist" {
  program = ["bash", "${path.root}/script/terraform-check-exist.sh"]

  query = {
    check_iam_policy= "true"
    iam_policy_arn= "arn:aws:iam::${module.common.account_id}:policy/${module.common.appmesh_k8s_iam_policy}"
  }
}

resource "aws_iam_policy" "appmesh_k8s_iam_policy" {
  name = module.common.appmesh_k8s_iam_policy
  path = "/"
  count = data.external.appmesh_k8s_iam_policy_exist.result.iam_policy_exist == "false" ? 1 : 0
  depends_on = [data.external.appmesh_k8s_iam_policy_exist]
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
        Resource = "arn:aws:iam::*:role/aws-service-role/appmesh.amazonaws.com/AWSServiceRoleForAppMesh",
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
