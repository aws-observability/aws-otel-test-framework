# Style Guide

## aotutil

- run `make fmt` before committing

## Java

- install `check style` plugin in your IDE
- import code style schema from [validator resource checkstyle](../validator/src/main/resources/checkstyle.xml)

## Terraform

- run `terraform fmt -recursive` or `cd terraform && make fmt`
- you can also use the `Terraform Tools` from context menu by right click on tf files,
  See [this comment for screenshot](https://github.com/aws-observability/aws-otel-test-framework/issues/265#issuecomment-813662105)

## Markdown

- use the markdown plugin from jetbrains IDE's to do the format, it enforces line wraps etc.