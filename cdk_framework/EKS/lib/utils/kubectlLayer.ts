import { KubectlV22Layer } from "@aws-cdk/lambda-layer-kubectl-v22";
import { KubectlV23Layer } from "@aws-cdk/lambda-layer-kubectl-v23";
import { ILayerVersion } from "aws-cdk-lib/aws-lambda";
import { KubectlLayer } from "aws-cdk-lib/lambda-layer-kubectl";
import { Construct } from "constructs/lib/construct";

export function GetLayer(scope: Construct, k8sVersion : string):  ILayerVersion{
    switch (k8sVersion){
      case 'v1.22':
        return new KubectlV22Layer(scope, 'v22Layer');
      case 'v1.23':
        return  new KubectlV23Layer(scope,'v23Layer');
      default:
        return new KubectlLayer(scope, 'v20Layer');
    }
  }