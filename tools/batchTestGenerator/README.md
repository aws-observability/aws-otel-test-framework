# Batch Test Generator

Creates batches of test cases for use locally or in a GitHub action.

## Commands
### local
Outputs a `test-case-batch` file.
### github
Sets the `batch-key` and `batch-values` output using std out. 

## Flags
### Globally Available flags
#### --testCaseFilePath
Path to test case file.
#### --include
List of services to include. If not provided will default to all.
#### --eksarm64amp
Endpoint for EKS ARM 64 AMP workspace.
#### --eksarm64cluster
Cluster name for EKS ARM 64 tests.
#### --eksarm64region
Region for EKS ARM 64 tests.
#### --eksamp
Endpoint for EKS AMP workspace.
#### --ekscluster
Cluster name for EKS tests.
#### --eksregion
Region for EKS tests.

### GitHub command unique
#### --maxBatch
Max number of batches to use.  

### Local command unique
#### --output
Output directory for `test-case-batch` file.
#### --maxJobs
Max number of jobs to include in `test-case-batch` file. 


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