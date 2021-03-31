all: license fmt build

fmt:
	goimports -d -l -w .

build: fmt
	go build .

license:
	addlicense -c 'Amazon.com, Inc. or its affiliates' $(shell ls *.go)