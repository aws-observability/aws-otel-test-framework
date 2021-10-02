## AWS Otel Testing Framework HLD

### 1. Background

This doc elaborates the high level design of the testing framework.

#### 1.1 Goals

* Support test cases for both AWS Otel Collector and Otel SDKs, test cases include end to end test, performance/soaking test and canary test. 
* Support multiple platforms: EC2, ECS/FARGATE, EKS/FARGATE
* Support multiple backends to validate: CloudWatch, XRay, Managed Prometheus.
* Be able to integrate with Github Workflow, but shouldn't be coupled.
* Be able to run locally for debugging.

### 2. Components

The framework combines multiple types of components, each type has multiple implementations, different implementations from different types of components construct different test cases. 
Below diagram outlines all the components in this framework and how they are interacted with each other.

![alt text](https://testing-framework-artifacts.s3.us-west-2.amazonaws.com/aot.drawio.png)

There are two logic paths in the diagram,

1. Provisioning Path[Red], In this path, Terraform load configuration from a test case, launch platform (Ex. EC2), and install Sample App as well collector in the platform, then launch validator.
2. Validation Path[Green], Once Validator starts, it sends requests to Sample App to generate metrics/traces, Sample App send those data to Collector, Collector send data to backend (Ex. CloudWatch), Validator calls backend to fetch data back for validation.

#### 2.1 Sample App (https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/sample-apps)

A Sample App is an application built with metrics/traces SDK, it runs as a container for purpose of supporting multiple computing platforms. Sample App generates metrics/trace data, sends them to AWS Otel Collector. There are two types of Sample App in general, 
1. A web application which serves some APIs, Validator call those APIs and then the web application generates metrics or trace data. Ex, [StatsD Sample App](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/sample-apps/statsd)
2. Self Emitting application which generates metrics or trace data once it starts. Ex, [Performance Load Generator](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/load-generator)

Sample app can be ignored for some test cases where the plugins in AWS Otel Collector generates metrics/traces automatically. Ex, [ECS Metric test](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases/ecsmetrics)

#### 2.2 Collector

A software which is running alongside with Sample App, receive data from Sample App and send to backends. The supported collector is AWS Otel Collector.

#### 2.3 Validator (https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/validator)

A Validator is a java application which calls Sample App to generate metrics/traces, fetch the metrics/traces from backend endpoints such as CloudWatch, XRay and Prometheus, thereby conducts validation on data schema and values. The supported backends for validation are 

1. CloudWatch
2. XRay
3. Managed Prometheus Endpoint

#### 2.4 Platform

A Platform is a place where Sample App and AWS Otel Collector are running, right now the supported platforms are

* EC2
* EC2 Based ECS
* FARGATE Based ECS
* EC2 Based EKS
* FARGATE Based EKS
* Local, Sample App and Collector run as containers in the same environment that the framework process is running.

see [here](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform) for more details.

#### 2.5 Test Case

A Test Case is the entrypoint of the framework, it defines which Sample App and which Validator to use. A test case runs on all the platforms by default.
Ex. the Statsd Sample App and CloudWatch Metric Validator are configured to test a case where Statsd receiver and EmfExporter are enabled in AWS Otel Collector.
[An example of test case definition](https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/terraform/testcases/xrayreceiver/parameters.tfvars)

#### 2.6 Mock Server

A mock server is a backend server running in the framework, it simulates the real backend with positive and negative situation, also enable the tests to be running in a non-credential environment. Ex. tests running for a PR.
The supported mock servers are
1. http
2. https
3. grpc

### 3. Integration with Github Workflow.

This section elaborates the general process of workflows in AWS Otel Collector and how testing framework integrate with it. 

#### 3.1 PR Build (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/PR-build.yml)

A PR creation triggers a PR build workflow to run, if the run fails, this PR can't be merged. 
Due to the limit that PR build can't involve credentials, The Testing framework is running with "Local platform" and Mock Server as backend. 

#### 3.2 CI (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/CI.yml)

Merging code to main branch triggers CI workflow, the general workflow is,
1. Build Artifacts. [RPM, DEB, MSI, Image]
2. Upload Artifacts into testing buckets&repos. [S3, ECR, Dockerhub]
3. Launch Testing framework to run all the test cases which are defined [here](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
4. If all the tests pass, upload artifacts to candidate buckets&repos.
5. If the code change contains version bumping, trigger soaking workflow.

#### 3.3 Soaking (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/soaking.yml)

A passed CI workflow with the version bumping triggers a soaking workflow. A regular soaking workflow runs every Saturday as well. The general workflow is,
Soaking test only runs on EC2 for easy performance tuning, soaking test uses Load-generator Sample app to generate high traffic data to collector, and use AlarmPullingValidator to ensure CPU and memory is normal.
Negative soaking test, which uses Mock server to simulate backend issues, is also running in this workflow.
1. Download Artifacts from candidate buckets.
2. Launch Testing framework to run all the soaking test cases configured [here](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
3. A failed soaking test creates an issue on Github.

#### 3.4 Performance (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/perf.yml)
Performance test runs on every Sunday, the test cases are same as soaking test, the difference is the testing duration is shorter and this workflow generates a performance model in the end.
The general workflow is,
1. Download Artifacts from candidate buckets. 
2. Launch Testing Framework to run all the performance test cases configured [here](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
3. Create an issue with performance model report. [Example of issue](https://github.com/aws-observability/aws-otel-collector/pull/637/files)

#### 3.5 CD (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/CD.yml)
CD workflow is to release Collector, it can only be manually triggered in terms of safety. It runs the similar test cases as CI workflow runs, the difference is that CD workflow run test cases right after release to ensure the released artifacts have no issues.
The general workflow is,
1. Download artifacts from candidate buckets. 
2. Release artifacts. [S3, ECR, Dockerhub]
3. Launch Testing Framework to run test cases for release validation, which are defined [here](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
4. Generate release notes based on the diff. [Example of release notes](https://github.com/aws-observability/aws-otel-collector/releases/tag/v0.13.0)

#### 3.6 Canary (https://github.com/aws-observability/aws-otel-collector/blob/main/.github/workflows/canary.yml)
Canary workflow runs every one hour, the test cases are running upon the released artifacts, Canary workflow generates metrics, a failed workflow trigger alarms, thereby high priority tickets.
The general workflow is,
1. Launch Testing Framework to run test cases, which are defined [here](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
2. Validator emits metrics to indicate the success or failure of the workflow running status.


### 4. Problems 

#### 4.1 Github Workflow can't retry a single job/test case

Workflows in AWS Otel Collector has 200+ test cases and sometimes those test cases are not stable due to network or environment issues,
the pain point is that it takes 1 hour to finish all the test cases but Github workflow does not support retry on a specific test case, this prevents us from effectively developing and testing. 
To resolve this issue, We invented a cache mechanism which stores the test case running status so that the next time the whole workflow retries, it skips the test cases which succeed in the last run. 


