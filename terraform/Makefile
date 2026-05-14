cleanupDirs = ecs ec2 eks 

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
	cat test-case-batch | xargs -L1 -P20 ./checkCacheHit.sh || true
	@if [ -s .cache-misses ]; then echo "Cache misses found:"; cat .cache-misses; exit 1; fi

.PHONY: terraformCleanup
terraformCleanup: test-case-batch
	cat test-case-batch | xargs -L1 -P1 ./executeTerraformCleanup.sh 

 

