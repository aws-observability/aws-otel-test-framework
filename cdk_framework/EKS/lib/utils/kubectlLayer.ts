import { KubectlV22Layer } from "@aws-cdk/lambda-layer-kubectl-v22";
import { KubectlV23Layer } from "@aws-cdk/lambda-layer-kubectl-v23";
import { KubectlLayer } from "@aws-cdk/lambda-layer-kubectl-v21";
import { aws_eks } from "aws-cdk-lib";
import { ILayerVersion } from "aws-cdk-lib/aws-lambda";
import { Construct } from "constructs/lib/construct";

export function GetLayer(scope: Construct, k8sVersion : aws_eks.KubernetesVersion):  ILayerVersion{
    switch (k8sVersion){
      case aws_eks.KubernetesVersion.V1_22:
        return new KubectlV22Layer(scope, 'v22Layer');
      case aws_eks.KubernetesVersion.V1_23:
        return  new KubectlV23Layer(scope,'v23Layer');
      case aws_eks.KubernetesVersion.V1_21:
        return new KubectlLayer(scope, 'v21Layer');
      default:
        throw new Error(`invalid kubernetes version: ${k8sVersion.version}`)
    }
  }
