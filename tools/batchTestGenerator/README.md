# Batch Test Generator

Create batches of test cases for use locally or in a GitHub action.

## Commands
### local
Outputs a `test-case-batch` file.
### github
Sets the `batch-key` and `batch-values` output using std out. 
### validate
Sets the `release-candidate-ready` GitHub output to `true` if all test cases are present in the DDB cache provided.
Users should provide identical values for `EKS` and `EKS_ARM_64` flags that were used when batches were generated. 

## Flags
### Globally Available flags
#### --testCaseFilePath
Path to test case file. Defaults to `./testcases.json`
See [this doc](inputFileSyntax.md) for the required format of the input file.

#### --include
List of services to include. If not provided will default to all.
Valid values are as follows "EKS", "ECS", "EC2", "EKS_ARM64", "EKS_ADOT_OPERATOR", "EKS_ADOT_OPERATOR_ARM64", "EKS_FARGATE"

### GitHub command unique
#### --maxBatch
Max number of batches to use.  


### Local command unique
#### --output
Output directory for `test-case-batch` file.
#### --maxJobs
Max number of jobs to include in `test-case-batch` file. 

### Validate command unique
### --ddbTable
Name of the DynamoDB table that should be queried for cache hits
### --aocVersion
Image tag for ADOT Collector version that was used in testing.



## Example Usage
```
./batchTestGenerator github  --testCaseFilePath=./testcases.json \
--include=ECS \
--maxBatch=2
```

```
./batchTestGenerator local --testCaseFilePath=./testcases.json \
--output=./ \
--maxJobs=10 \
--include=ECS,EKS
```

```
./batchTestGenerator validate --testCaseFilePath=./testcases.json \
--ddbtable=BatchTestCache \
--aocVersion=v0.17.0-1e2c593 \
--include=ECS
```


