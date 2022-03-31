all: license fmt build

fmt:
	go install golang.org/x/tools/cmd/goimports@latest
	goimports -d -l -w .

build:
	go build .

install: fmt
	go install .

license:
	addlicense -c 'Amazon.com, Inc. or its affiliates' $(shell ls *.go)