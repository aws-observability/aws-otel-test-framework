all: license fmt build

fmt:
	goimports -d -l -w .

build: fmt
	go build .

install: fmt
	go install .

license:
	addlicense -c 'Amazon.com, Inc. or its affiliates' $(shell ls *.go)