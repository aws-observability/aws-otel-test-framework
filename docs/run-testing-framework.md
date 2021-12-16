## 1. Run testing framework locally
 
1. Clone this repo:
````
git clone git@github.com:aws-observability/aws-otel-test-framework.git
````
2. Clone the AWS OpenTelemetry Collector repo:
````
git clone git@github.com:aws-observability/aws-otel-collector.git
````
3. Install Terraform: https://learn.hashicorp.com/tutorials/terraform/install-cli
 
4. Install Docker compose: https://docs.docker.com/compose/install/
 
5. Run one of the test cases:
````shell script
cd aws-otel-test-framework/terraform/mock
terraform init
terraform apply -var="testcase=../testcases/otlp_mock" 
terraform destroy
````
 
#### What does the test do?
 
1. Builds collector image from the directory ../aws-otel-collector
2. Runs the collector, sample app, and mock server in docker.
3. Validates if the mock server receives data from collector.
 
 
## 2. Run test cases in AWS platform
 
In the case that you want to debug for a certain platform, you can also use this testing framework to run your test case locally in multiple AWS platforms including EC2, ECS, and EKS.
 
### 2.1 Prerequisite
 
#### 2.1.1 Setup your aws credentials
Refer to: https://docs.aws.amazon.com/cli/latest/userguide/cli-configure-files.html
 
#### 2.1.2 Run Setup
Setup only needs to be run once, it creates:
 
1. one iam role
2. one vpc
3. one security group
4. two ecr repos, one for sample apps, one for mocked server
 
Run 
````
cd terraform/setup && terraform init && terraform apply
````
 
And Run
````
cd terraform/imagebuild && terraform init && terraform apply
````
this task will build and push the sample apps and mocked server images to the ecr repos,
 so that the following test could use them.
 
Remember, if you have changes on sample apps or the mocked server, you need to rerun this imagebuild task.
 
#### 2.1.3 Build Image
Please follow https://github.com/aws-observability/aws-otel-collector/blob/main/docs/developers/build-docker.md to build your image with the new component, and push this image to dockerhub, record the image link, which will be used in your testing.

#### 2.2 Run in EC2
````
cd terraform/ec2 && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo=<your_image_repo>" \
    -var="aoc_version={{the aoc binary version}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````

Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````

### 2.3 Run in ECS
 
````
cd terraform/ecs && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo={{the docker image repo name you just pushed}}" \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````
 
Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````
 
### 2.4 Run in EKS
Prerequisite: you are required to create an EKS cluster in your account
````
cd terraform/eks && terraform init && terraform apply -auto-approve \
    -var="aoc_version={{ the docker image tag name}}" \
    -var="aoc_image_repo={{the docker image you just pushed}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars" \
    -var="eks_cluster_name={the eks cluster name in your account}" 
````

Don't forget to clean up your resources:
````
terraform destroy -auto-approve \
    -var="eks_cluster_name={the eks cluster name in your account}"
````

### 2.4.1 Run in EKS Fargate
#### Set Up
* Install kubectl and eksctl
  * https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html
  * https://docs.aws.amazon.com/eks/latest/userguide/getting-started-eksctl.html
* Create eks fargate cluster
```
eksctl create cluster --name <cluster_name> --region <region> --fargate
```
* Create a oicd 
```
eksctl utils associate-iam-oidc-provider --cluster <cluster_name> --approve
```
* Create web identity role (ServiceAccount-eks-test-aoc-role)
  * Identity provider is the openId connect on your eks cluster
  * Add required permissions for the collector for your test
* Create Ingress controller fargate
  * https://aws.amazon.com/premiumsupport/knowledge-center/eks-alb-ingress-controller-fargate/
  * To test that ingress set up
    * Download config https://github.com/kubernetes-sigs/aws-load-balancer-controller/blob/main/docs/examples/2048/2048_full.yaml and replace all instances of namespace game-2048 with default.
    * Check for external url after a couple of minutes
      * ````kubectl get ingress````
#### How to run fargate tests
Add -var="deployment_type=fargate" to the eks creation statement
Supported tests
* otlp_mock

Not supported tests
* otlp_trace
  * This is because no sts role given to the sample app. 

Test
```
cd terraform/eks && terraform apply -auto-approve \
  -var="aoc_image_repo=<your_image_repo>" \
  -var="aoc_version={{ the docker image tag name}}" \
  -var="testcase=../testcases/<your_testcase>" \
  -var-file="../testcases/<your_testcase>/parameters.tfvars" \
  -var="eks_cluster_name=<your_cluster>" \
  -var="deployment_type=fargate"
```

Don't forget to clean up your resources:
````
terraform destroy -auto-approve \
    -var="cluster_name=<you_cluster_name>" \
    -var="deployment_type=fargate"
````

#### 2.5 Run in canary
````
cd terraform/canary && terraform init && terraform apply -auto-approve \
    -var="aoc_image_repo=<your_image_repo>" \
    -var="aoc_version={{ the aoc binary version}}" \
    -var="testcase=../testcases/{{your test case folder name}}" \
    -var-file="../testcases/{{your test case folder name}}/parameters.tfvars"
````
 
Don't forget to clean up your resources:
````
terraform destroy -auto-approve
````
