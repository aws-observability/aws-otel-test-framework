CLUSTERS := $(shell cdk ls)
EKSCLUSTERS := $(shell printf '%s' '$(CLUSTERS)' | grep -oh "\w*EKSCluster\w*")
len := $(shell printf '%s' '$(EKSCLUSTERS)' | wc -w)

EKS-infra:
	make deploy-VPC
	make -j $(len) deploy-cluster
.PHONY: local

deploy-cluster: $(EKSCLUSTERS:%=deploy-cluster/%)
deploy-cluster/%: CLUSTER=$*
deploy-cluster/%:
	cdk deploy $(CLUSTER) --require-approval never

deploy-VPC:
	cdk synth
	cdk deploy EKSVpc
.PHONY: deploy-VPC

check-clusters:
	@echo $(CLUSTERS)

check-eksclusters:
	@echo $(EKSCLUSTERS)


