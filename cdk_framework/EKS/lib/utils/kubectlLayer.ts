import { KubectlV22Layer } from "@aws-cdk/lambda-layer-kubectl-v22";
import { KubectlV23Layer } from "@aws-cdk/lambda-layer-kubectl-v23";
import { aws_eks } from "aws-cdk-lib";
import { ILayerVersion } from "aws-cdk-lib/aws-lambda";
import { KubectlLayer } from "aws-cdk-lib/lambda-layer-kubectl";
import { Construct } from "constructs/lib/construct";

export function GetLayer(scope: Construct, k8sVersion : aws_eks.KubernetesVersion):  ILayerVersion{
    switch (k8sVersion){
      case aws_eks.KubernetesVersion.V1_22:
        return new KubectlV22Layer(scope, 'v22Layer');
      case aws_eks.KubernetesVersion.V1_23:
        return  new KubectlV23Layer(scope,'v23Layer');
      default:
        return new KubectlLayer(scope, 'v20Layer');
    }
  }