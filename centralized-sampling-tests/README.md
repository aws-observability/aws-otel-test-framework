# Centralized Sampling Integration Tests

## Introduction

This project is meant to be used to test that X-ray's centralized sampling strategies
are working properly. This folder has sample apps configured for the integration tests
under sample apps in languages that support centralized sampling. As of now the languages
supported are Java and Go. To run these tests, first start up the collector, then
start up the chosen sample app, then start running the tests.

## Run Locally

### Set up collector
To run locally first set up the collector with the correct configuration.
It is possible that this is already done. Available ADOT Collector releases can be found here
[aws-otel-collector](https://github.com/aws-observability/aws-otel-collector/releases).
Make sure that the collector config is configured to work with a local x-ray listener pointed
to port 2000 and the docker run command exposes port 2000. Set up the ADOT collector with the 
example-collector-config file. Start the collector with command.
```shell
    docker run --rm -p 2000:2000 -p 55680:55680 -p 8889:8888 \
      -e AWS_REGION=us-west-2 \
      -e AWS_PROFILE=default \
      -v ~/.aws:/root/.aws \
      -v "${PWD}/example-collector-config.yaml":/otel-local-config.yaml \
      --name awscollector public.ecr.aws/aws-observability/aws-otel-collector:latest \
      --config otel-local-config.yaml;
```

### Start up sample app
Start up the sample app of your choice in the sample apps folder. The sample apps exist in the sample-apps folder. 
Each sample app will have a readMe on how to run it. If adding a sample-app to use for the integration tests see
[Sample-app-requirements](https://docs.google.com/document/d/1nu6XwYKe8h3EZ6upCQqf83hI9gQ-yg5WXlxHRjJ7BCg/edit?usp=sharing)
. The sample apps were manually instrumented for X-Ray Remote Sampling, for more context see
[here](https://aws-otel.github.io/docs/getting-started/java-sdk/trace-auto-instr#using-x-ray-remote-sampling)

### Start up integration tests
run this command in the root dir of the testing framework once collector and sample app are up and running
```shell
./gradlew :centralized-sampling-tests:integration-tests:run
```
