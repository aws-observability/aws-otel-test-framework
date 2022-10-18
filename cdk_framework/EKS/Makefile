CLUSTERS := $(shell cdk ls)
EKSCLUSTERS := $(shell printf '%s' '$(CLUSTERS)' | grep -oh "\w*EKSCluster\w*")
len := $(shell printf '%s' '$(EKSCLUSTERS)' | wc -w)

EKS-infra:
	make deploy-VPC
	make -j $(len) deploy-clusters
.PHONY: local

deploy-clusters: $(EKSCLUSTERS:%=deploy-clusters/%)
deploy-clusters/%: CLUSTER=$*
deploy-clusters/%:
	cdk deploy $(CLUSTER) --require-approval never

deploy-VPC:
	cdk synth
	cdk deploy EKSVpc
.PHONY: deploy-VPC
