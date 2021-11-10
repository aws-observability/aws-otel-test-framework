ADDLICENSE=$(PWD)/bin/addlicense

.PHONY: install-tools
install-tools:
	GOBIN=$(PWD)/bin go install google.golang.org/protobuf/cmd/protoc-gen-go@v1.26
	GOBIN=$(PWD)/bin go install google.golang.org/grpc/cmd/protoc-gen-go-grpc@v1.1
	GOBIN=$(PWD)/bin go install github.com/google/addlicense@latest

.PHONY: gen-proto
gen-proto:
	PATH="$(PATH):$(PWD)/bin" protoc --go_out=. --go_opt=paths=source_relative \
		--go-grpc_out=. --go-grpc_opt=paths=source_relative \
		**/proto/*.proto

.PHONY: license
license:
	$(ADDLICENSE) -c 'Amazon.com, Inc. or its affiliates' $(shell ls **/*.go)