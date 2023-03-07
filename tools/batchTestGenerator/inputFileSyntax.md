# Input File Syntax
The input file represents a set of test cases, platforms, and EKS cluster targets. Each test case can result a 1 to many
combination of `case_name` to `platform`. In the case of EKS platforms additional permutations are created for each 
`clustertarget` that is attached to that EKS platform type. This input file is read in by the batch test generator to
create these permutations and then break them up into smaller batches. 

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
          "region": "us-east-2",
          "excluded_tests": [
            "otlp_metric"
          ]
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
    },
    {
      "case_name": "otlp_metric",
      "platforms": [
        "EC2",
        "ECS",
        "EKS",
        "EKS_ARM64",
        "CANARY"
      ]
    }
  ]
}
```

### clustertargets
Define a set of clusters to dispatch EKS tests to. Each EKS platform that is included in your
test case platforms must have cluster targets defined. A test will be run against all corresponding
cluster targets if that service is included in the batch generation. Defining a test case with `EKS_FARGATE` tests
without defining a cluster target with type `EKS_FARGATE` will result in an error. Test cases can also be excluded 
from executing on a certain cluster by using the `excluded_tests` field. 

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