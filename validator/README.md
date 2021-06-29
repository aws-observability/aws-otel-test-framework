# Validator
validator is one of the component of the testing framework, and can be run independently.

## Run
### run as a command

run this command in the root dir of the testing framework

```shell
./gradlew :validator:run --args='-c default-otel-trace-validation.yml --endpoint http://127.0.0.1:4567'
```

help

```shell
./gradlew :validator:run --args='-h'
```

### run as github action

```yaml
uses: aws-observability/aws-otel-collector-test-framework@terraform
with:
    running_type: validator
    opts: "-c default-otel-trace-validation.yml --endpoint 'the endpoint to test(Ex. 127.0.0.1:4567)'"
```

## Add a validation suite

1. add a config file under `resources/validations`
2. add an expected data under `resources/expected-data-template`