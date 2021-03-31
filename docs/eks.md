# EKS

## Overview

There is only on EKS EC2 cluster (for now), the cluster is created manually using `eksctl`

## Create Cluster

The IAM policy is pretty big because it supports the following backends

- CloudWatch
- Xray
- Managed Grafana

We need SSM for [patching](aws-patch.md)

```yaml
# eksctl create cluster -f aws-otel-testing-framework-eks.yaml
# aws eks update-kubeconfig --name aws-otel-testing-framework-eks --region us-west-2
apiVersion: eksctl.io/v1alpha5
kind: ClusterConfig

metadata:
  name: aws-otel-testing-framework-eks
  region: us-west-2
  version: '1.18'

# You can remove vpc section and eksctl will create a new one
vpc:
  subnets:
    public:
      us-west-2a: { id: subnet-yourid }
      us-west-2b: { id: subnet-yourid }
      us-west-2c: { id: subnet-yourid }

managedNodeGroups:
  - name: mng-m5large
    instanceType: m5.xlarge
    desiredCapacity: 6
    volumeSize: 256
    iam:
      attachPolicyARNs:
        - arn:aws:iam::aws:policy/AmazonEKSWorkerNodePolicy
        - arn:aws:iam::aws:policy/AmazonEKS_CNI_Policy
        - arn:aws:iam::aws:policy/AmazonEC2ContainerRegistryReadOnly
        - arn:aws:iam::aws:policy/CloudWatchAgentServerPolicy # CWAgent
        - arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore # SSM
        - arn:aws:iam::aws:policy/AWSXrayWriteOnlyAccess # xray
        - arn:aws:iam::aws:policy/AWSXRayDaemonWriteAccess # xray
        - arn:aws:iam::aws:policy/AmazonS3ReadOnlyAccess # s3
        - arn:aws:iam::aws:policy/AmazonPrometheusFullAccess # amp, not sure if `AmazonPrometheusRemoteWriteAccess` works ...
    tags:
      creator: someone
    ssh:
      publicKeyName: your-ssh-keypair
      allow: true

cloudWatch:
  clusterLogging:
    enableTypes: [ "*" ]
```