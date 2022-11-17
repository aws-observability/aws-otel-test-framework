import { KubectlV22Layer } from '@aws-cdk/lambda-layer-kubectl-v22';
import { KubectlV23Layer } from '@aws-cdk/lambda-layer-kubectl-v23';
import { KubectlLayer } from '@aws-cdk/lambda-layer-kubectl-v21';
import { aws_eks } from 'aws-cdk-lib';
import { ILayerVersion } from 'aws-cdk-lib/aws-lambda';
import { Construct } from 'constructs/lib/construct';
import { KubectlV24Layer } from '@aws-cdk/lambda-layer-kubectl-v24';

export function GetLayer(
  scope: Construct,
  k8sVersion: aws_eks.KubernetesVersion
): ILayerVersion {
  switch (k8sVersion.version) {
    case '1.21':
      return new KubectlLayer(scope, 'v21Layer');
    case '1.22':
      return new KubectlV22Layer(scope, 'v22Layer');
    case '1.23':
      return new KubectlV23Layer(scope, 'v23Layer');
    case '1.24':
      return new KubectlV24Layer(scope, 'v24Layer');
    default:
      throw new Error(`invalid kubernetes version: ${k8sVersion.version}`);
  }
}
