# Centralized Sampling Integration Tests

## Run
To run the integration tests there are two requirements for the tests
to successfully run. First there must be the ADOT collector running with 
aws xray on port 2000, i.e. Collector running at http://localhost:2000. Secondly
there must be one of the sample apps configured for centralized sampling
running on port 8080 i.e. sample app running at http://localhost:8080. If either
of these components are missing the tests will fail and throw an IOException
immediately. For full instructions on how to run all components of the test 
see the parent folders README.

### run as a command
```shell
./gradlew :centralized-sampling-tests:integration-tests:run
```

### run as a github action 
to be determined
