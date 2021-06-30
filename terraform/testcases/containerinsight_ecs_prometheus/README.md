# ContainerInsight ECS Prometheus

## Overview

This is e2e test
for [extension/ecsobserver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/extension/observer/ecsobserver)

## Usage

If your AWS account is `123456` and you want to run your own image
`123456.dkr.ecr.us-west-2.amazonaws.com/aoc:myfeature-0.2`.

- `ecs_launch_type` is the launch type for aoc itself, launch type for extra apps are defined
  in [parameters.tfvars](parameters.tfvars)
- `aoc_image_repo` is repo for aoc without tag
- `aoc_version` is the image tag for aoc
- `ecs_extra_apps_image_repo` is the repo for all the extra apps, remaining part of image name and version are defined
  as tag in templates like [jmx.json](jmx.json)
  e.g. `123456.dkr.ecr.us-west-2.amazonaws.com/prometheus-samples:tomcat-jmx-latest`

```bash
cd $PROJECT/terraform/ecs
terraform apply \
  -var="ecs_launch_type=FARGATE" \
  -var="disable_mocked_server=true" \
  -var="aoc_version=myfeature-0.2" \
  -var="aoc_image_repo=123456.dkr.ecr.us-west-2.amazonaws.com/aoc" \
  -var="ecs_extra_apps_image_repo=123456.dkr.ecr.us-west-2.amazonaws.com/prometheus-samples" \
  -var="testcase=../testcases/containerinsight_ecs_prometheus" \
  -var-file="../testcases/containerinsight_ecs_prometheus/parameters.tfvars" 
```

## Development

### Files

- [ecs_taskdef.tpl](ecs_taskdef.tpl) is override for [default](../../templates/defaults/ecs_taskdef.tpl) because we
  don't need mock server and sample app, i.e., not deploying collector as sidecar.
    - log group is `/ecs/ecs-adot-collector-service`

### Build and push sample app image

There are multiple sample applications

### Validation

```bash
# Run at project root to make sure the validator code pass style check and compiles
./gradlew :validator:build
# Run at terraform/ecs to run validation without spinning up new infra
make validate
```

- validation config file name is specified
  in [parameters.tfvars](parameters.tfvars) `ecs-container-insight-prometheus.yml`
- the actual validation config is located
  in [validator/src/main/resources/validations](../../../validator/src/main/resources/validations/ecs-container-insight-prometheus.yml)
- path to log and metrics validation templates is
  in [PredefinedExpectedTemplate](../../../validator/src/main/java/com/amazon/aoc/fileconfigs/PredefinedExpectedTemplate.java)
  while the actual files are
  in [expected-data-template/container-insight/ecs/prometheus](../../../validator/src/main/resources/expected-data-template/container-insight/ecs/prometheus)
- `validationType: "container-insight-ecs-prometheus-logs"` in config
  triggers [ValidatorFactory](../../../validator/src/main/java/com/amazon/aoc/validators/ValidatorFactory.java)

## Problems

List of common problems you may encounter.

### CloudWatch Context

Some tests have a different `ecs_taskdef_directory` e.g. [prometheus_mock](../prometheus_mock/parameters.tfvars). And
you need to put a dummy `cloudwatch_context.json` in that directory.

```text
Error: Invalid function argument

  on main.tf line 378, in data "template_file" "cloudwatch_context":
 378:   template = file(local.cloudwatch_context_path)
    |----------------
    | local.cloudwatch_context_path is "../templates/prometheus/cloudwatch_context.json"
```

### Unknown variable

You are using wrong variable name in template files, or you didn't define the var when rendering template.

```
Unknown variable; There is no variable named
```