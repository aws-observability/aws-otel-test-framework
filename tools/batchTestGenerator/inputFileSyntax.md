# Input File Syntax
## Example
```json
{
  "clustertargets": [
    {
      "type": "EKS_ARM64",
      "targets": [
        {
          "name": "eks-cluster-arm64-1",
          "region": "us-west-2"
        },
        {
          "name": "eks-cluster-arm64-2",
          "region": "us-east-2"
        }
      ]
    },
    {
      "type": "EKS",
      "targets": [
        {
          "name": "eks-cluster-1",
          "region": "us-east-1"
        }
      ]
    }
  ],
  "tests": [
    {
      "case_name": "xrayreceiver",
      "platforms": [
        "EC2",
        "ECS",
        "EKS",
        "EKS_ARM64",
        "EKS",
        "CANARY"
      ]
    }
  ]
}
```

### clustertargets
Define a set of clusters to dispatch EKS tests to. Each EKS platform that is included in your
test case platforms must have cluster targets defined. Defining a test case with `EKS_FARGATE` tests
without defining a cluster target with type `EKS_FARGATE` will result in an error. 

### tests
Define an array of test cases. 
Valid platform values are:
```text
EC2
ECS
EKS
EKS_ARM64
EKS_FARGATE
EKS_ADOT_OPERATOR
EKS_ADOT_OPERATOR_ARM64
CANARY
PERF
LOCAL
```