import { aws_eks } from 'aws-cdk-lib';
import { ILayerVersion } from 'aws-cdk-lib/aws-lambda';
import { Construct } from 'constructs/lib/construct';
import { KubectlV29Layer } from '@aws-cdk/lambda-layer-kubectl-v29';
import { KubectlV30Layer } from '@aws-cdk/lambda-layer-kubectl-v30';
import { KubectlV31Layer } from '@aws-cdk/lambda-layer-kubectl-v31';
import { KubectlV32Layer } from '@aws-cdk/lambda-layer-kubectl-v32';
import { KubectlV33Layer } from '@aws-cdk/lambda-layer-kubectl-v33';
import { KubectlV34Layer } from '@aws-cdk/lambda-layer-kubectl-v34';

export function GetLayer(
  scope: Construct,
  k8sVersion: aws_eks.KubernetesVersion
): ILayerVersion {
  switch (k8sVersion.version) {
    case '1.29':
      return new KubectlV29Layer(scope, 'v29Layer');
    case '1.30':
      return new KubectlV30Layer(scope, 'v30Layer');
    case '1.31':
      return new KubectlV31Layer(scope, 'v31Layer');
    case '1.32':
      return new KubectlV32Layer(scope, 'v32Layer');
    case '1.33':
      return new KubectlV33Layer(scope, 'v33Layer');
    case '1.34':
      return new KubectlV34Layer(scope, 'v34Layer');
    default:
      throw new Error(`invalid kubernetes version: ${k8sVersion.version}`);
  }
}
