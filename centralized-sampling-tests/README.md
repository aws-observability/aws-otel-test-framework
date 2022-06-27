# Centralized Sampling Integration Tests

## Introduction

This project is meant to be used to test that X-ray's centralized sampling strategies
are working properly. This folder has sample apps configured for the integration tests
under sample apps in languages that support centralized sampling. As of now the languages
supported are Java and Go. To run these tests, first start up the collector, then
start up the chosen sample app, then start running the tests.


The design doc for this project is found here
[Design Doc](https://quip-amazon.com/qcsdACbyatNi/X-Ray-Centralized-Sampling-Testing).
The wiki for this project is found here
[Wiki](https://w.amazon.com/bin/view/AWS/X-Ray/Development/SDK_and_Tools/Centralized-Sampling-Integration-Testing)

## Run Locally

### Set up collector
To run locally first set up the collector with the correct configuration.
It is possible that this is already done. The collector-contrib releases can be found here
[Otel-Collector-Contrib](https://github.com/open-telemetry/opentelemetry-collector-contrib/releases).
Make sure that the collector config is configured to work with a local x-ray listener. 
See the example-collector-config.yaml for what it should look like. Start the collector.

### Start up sample app
Start up the sample app of your choice that is configured for centralized sampling integration
tests. The sample apps exist in the sample-apps folder. Each sample app will have a 
readMe on how to run it. If adding a sample-app to use for the integration tests see
[Sample-app-requirements](https://quip-amazon.com/o48yA4tsDf4F/Integration-Test-Sample-App-Requirements)

### Start up integration tests
Insert directions on how to run once they are done.

