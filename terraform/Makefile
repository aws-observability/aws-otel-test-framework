
.PHONY: fmt
fmt:
	terraform fmt -recursive

.PHONY: check-fmt
check-fmt:
	terraform fmt -recursive -check

.PHONY: execute-batch-test
execute-batch-test: test-case-batch
	cat test-case-batch | xargs -L1 -P1 ./executeTerraformTest.sh

.PHONY: test-case-batch-test
test-case-batch-test: test-case-batch
	cat test-case-batch | xargs -L1 -P1 echo

.PHONY: postBatchClean
postBatchClean:
	rm -rf ./tmp

.PHONY: checkCacheHits
checkCacheHits:
	cat test-case-batch | xargs -L1 -P1 ./checkCacheHit.sh
 

