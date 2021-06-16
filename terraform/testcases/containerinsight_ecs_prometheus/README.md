# ContainerInsight ECS Prometheus

## Overview

This is e2e test
for [extension/ecsobserver](https://github.com/open-telemetry/opentelemetry-collector-contrib/tree/main/extension/observer/ecsobserver)

## TODO

- [ ] [cloudwatch_context.json](cloudwatch_context.json)
    - [ ] we need to update java code to include `taskDefinitionFamily` and `serviceName`
- [x] app image repo is hardcoded and not used
- [ ] log group name for sample applications' container log
- [ ] why I created a new `ecs_taskdef.tpl` is it used?

## Usage

Non default image `123456.dkr.ecr.us-west-2.amazonaws.com/aoc:myfeature-0.2`

- `ecs_launch_type` is the launch type for aoc itself, launch type for extra apps are defined in TODO(where?)
- `aoc_image_repo` is repo for aoc without tag
- `aoc_version` is the image tag for aoc
- `ecs_extra_apps_image_repo` is the repo for all the extra apps, remaining part of image name and version are defined
  as tag in templates like [jmx.json](jmx.json)
  e.g. `123456.dkr.ecr.us-west-2.amazonaws.com/prometheus-samples:tomcat-jmx-latest`

```bash
terraform apply \
  -var="ecs_launch_type=FARGATE" \
  -var="disable_efs=true" \
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
