CMD_DIR := $(abspath ./cmd)
ALL_GO_MOD_DIRS := $(shell find . -type f -name 'go.mod' -exec dirname {} \; | sort)

GOTEST_OPT?= -short -coverprofile coverage.txt -v -race -timeout 180s
GOTEST=go test

build-aotutil:
	cd ${CMD_DIR}/aotutil && make build

.PHONY: list-mod
list-mod:
	@echo ${ALL_GO_SRC}

.PHONY: go-test
go-test:
	@set -e; for dir in $(ALL_GO_MOD_DIRS); do \
	  (cd "$${dir}" && \
	    go list ./... \
		  | xargs -n 10 $(GOTEST) $(GOTEST_OPT)) ; \
	done
