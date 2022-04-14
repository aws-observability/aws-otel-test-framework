
.PHONY: fmt
fmt:
  terraform fmt -recursive

.PHONY: check-fmt
check-fmt:
  terraform fmt -recursive -check

.PHONY: execute-batch-test
execute-batch-test: test-case-batch
  xargs -L1 -P1 -a test-case-batch ./executeTerraformTest.sh

.PHONY: postBatchClean
postBatchClean:
  rm -rf ./tmp
 