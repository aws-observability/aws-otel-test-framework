# AOT Testing Framework

Before adding a new component into AWS Otel Collector, we require contributors to add related end-to-end test cases, this project is a testing framework which helps contributors to easily add testing into AWS Otel Collector. 

* Contributors won't need to run the test locally to provide the testing result, instead, just define it. The test case will be automatically picked by the github workflow in AWS Otel Collector repo to run integration test and soaking test on different platforms. We will notify contributors if a certain test case is failed.

* Contributors won't need to build the validation logic for their own backends, no api keys or credentials are required. We use our own version of [mocked backend](mocked_server/) in the integration and soaking test. Inside this mock server, we only validate if the data is received but not the data accuracy. [The mechanism of testing framework](docs/mechanism-of-testing-framework.md)

* Contributors won't need to define different test cases for different platforms. The point is, one test case for multiple types of tests including integration test and soaking test. 

Any questions could be asked on [gitter](https://gitter.im/aws-observability-aws-otel-test-framework/community)

## 1. Two PRs to contribute your component to [AWS Otel Collector](https://github.com/aws-observability/aws-otel-collector)

Below are the steps to add a new component to AWS Otel Collector. Before doing this, please ensure your component is already merged into [opentelemetry-collector-contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib)

### 1.1 Create a PR to Define test case in testing framework Repo

[Example PR for http exporter](https://github.com/aws-observability/aws-otel-test-framework/pull/90)

#### 1.1.1 create test case folder
You will need to create a sub folder under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases).

We define all the test cases under [the testcase directory](https://github.com/aws-observability/aws-otel-test-framework/tree/terraform/terraform/testcases), and each sub folder will be treated as a test case. 


#### 1.1.2 place `otconfig.tpl` under test case folder
You will need to place a `otconfig.tpl` file under the test case folder, We require only one pipeline defined in the config for easy debuging, if your component supports both metric and trace, you will need two test cases.

there're three placeholders could be used:

* `${mocked_endpoint}`, which refers to the mocked_endpoint provided in the testing framework. [An example to use mock_endpoint to override the real backend](https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/terraform/testcases/otlp_http_exporter_metric_mock/otconfig.tpl#L15)

* `${grpc_port}`, which refers to the recevier grpc port allocated in the testing framework so that the throughput could be sent into the collector. [An example to use grpc_port](https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/terraform/testcases/otlp_http_exporter_metric_mock/otconfig.tpl#L5)

* `${region}`, which refers to the region where the test case will be running and validate. in normal case, region should be auto-detected so this placeholder might not be used in the config. [an example to use region](https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/terraform/testcases/otlp_metric/otconfig.tpl#L15)

Please note OTLP receiver is the only receiver which could be configured in the pipeline to serve the traffic at this moment. if you want to add a new receiver, please [contribute a sample app](sample-apps/README.md)

#### 1.1.3 place `parameters.tfvars` under test case folder

You will need to place a `parameters.tfvars` file under the test case folder to override [the default parameters](terraform/common.tf). Must have parameters are:

* `soaking_data_mode`: which tells the testing framework to send which types of data to collector for soaking test, the optional values are: `metric`, `trace`. Default value is `metric`. [An example to configure the data mode](https://github.com/aws-observability/aws-otel-test-framework/blob/terraform/terraform/testcases/otlp_http_exporter_trace_mock/parameters.tfvars#L2)


### 1.2 Create a PR to Link test case to AWS Otel Collector Repo

[Example PR for http exporter](https://github.com/aws-observability/aws-otel-collector/pull/124)

Once the first PR get merged, you will need to create a PR to AWS Otel Collector Repo, this PR needs to build the component and link test cases in one shot, which includes

* the changes to build the new component into AWS Otel Collector. 
* the changes to link the test cases. 

You will need to add a block in the [testcases.json](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json)
 with two fields. 

#### 1.2.1 case_name

`case_name` needs to be the test case folder name defined in the testing framework. [An example for otlp_http_exporter_trace_mock](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json#L27)

#### 1.2.2 platforms

`platforms` are the platforms the test case will be running on. We require all the platforms to be listed unless you have a special reason to skip some of the platforms. [An example for otlp_http_exporter_trace_mock](https://github.com/aws-observability/aws-otel-collector/blob/main/e2etest/testcases.json#L28) Below are the platforms: 

* LOCAL, which will run the test in the pr workflow, the test will be directly running on the github workflow vm.
* EC2, which will run the test in the main branch workflow under an ec2 instance.
* ECS, which will run the test in the main branch workflow under an ecs cluster.
* EKS, which will run the test in the main branch workflow under an eks/k8s cluster.
* SOAKING, which will run every night, and perform high throughput to your test case and monitor the resource usages.
* NEG_SOAKING, which will run every night, and perform high throughput to your test case with false endpoint and monitor the resource usages.

## 2. Run testing framework

If a certain test case is failed in the github workflow of AWS Otel Collector, you might need to debug the test case locally. 

* [run the local test](docs/run-testing-framework.md#1-run-testing-framework-locally)
* [run the ec2 test](docs/run-testing-framework.md#24-run-in-ec2-tbd)
* [run the ecs test](docs/run-testing-framework.md#22-run-in-ecs)
* [run the eks test](docs/run-testing-framework.md#23-run-in-eks)
* [run the soaking test](docs/run-testing-framework.md)
* [run the negative soaking test](docs/run-testing-framework.md)


## 3. Contributing

We have collected notes on how to contribute to this project in CONTRIBUTING.md.

## 4. Security

See [CONTRIBUTING](CONTRIBUTING.md#security-issue-notifications) for more information.

## 5. License

This project is licensed under the Apache-2.0 License.

