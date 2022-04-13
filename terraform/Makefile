ifeq ($(origin .RECIPEPREFIX), undefined)
  $(error This Make does not support .RECIPEPREFIX. Please use GNU Make 4.0 or later)
endif
.RECIPEPREFIX = >


.PHONY: fmt
fmt:
> terraform fmt -recursive

.PHONY: check-fmt
check-fmt:
> terraform fmt -recursive -check



.PHONY: execute-batch-test
execute-batch-test: test-case-batch
> xargs -L1 -P1 -a test-case-batch ./executeTerraformTest.sh