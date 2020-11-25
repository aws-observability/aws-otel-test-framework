# Testing Framework

## 1. Basic concepts

There're some concepts you might want to learn. 

### 1.1 the workflow of the testing framework

the testing framework is built based on terraform, every time you run your testing suite, it will

1. **Create resources** Create the related resources in your aws account, such as, ec2 instance/ecs cluster.
2. **Install softwares** Install AWS Otel Collector/sample apps onto it and start it base on the configurations you provide. 
3. **Validate data** Validate the data by fetching them from backend(CloudWatch, XRay) base on the validation config you provide. 


### 1.2 Parameter override

All the parameters defined under `ec2/variables.tf`, `ecs/variables.tf`, `eks/variables.tf` could be overrided. When you run the terraform command, give it a `-var-file=xxx.tfvars`, so that all the parameters in xxx.tfvars will override their default value in variables.tf. 

## 2. Parameters to override

* `aoc_version` is a parameter you can use while running the test, which tells the testing framework to fetch the AWS Otel Collector with this version. This parameter is used as the `tag` of the collector image. 
 
* `otconfig_path`, the path to your ot config file. 
* `validation_config`, the filename of the validation configuration, which is under `validator/src/main/resources/validations` folder.
* `sample_app_callable`, by default it's true, only set it to false when you don't have a web application sample app image.
* `data_emitter_image`, if you have a sample app then set its image name including tag here
* `aoc_image_repo`, if you have an AWS Otel Collector image you just built with the new component, then set its repo name here

an example here:

```shell
sample_app_callable = false

# this file is defined in validator/src/main/resources/validations
validation_config="statsd-metric-validation.yml"

data_emitter_image="alpine/socat:latest"
```

## 3. Placeholders

When you define the configuration files[otconfig, ecs task definition, Docker compose file, Eks Config, validation config], you can use some placeholders so that the testing framework will automatically fill in the values during runtime.


#### 3.1 otconfig

Below are the placeholders you can use in the otconfig

* region
* otel_service_namespace
* otel_service_name
* testing_id, which is a placeholder that you can use in your configuration, which is a unique id representing each run of the testing suite. You can use this testing id as part of your metric name, or dimension name, to ensure the metric emitted from each run of the testing suite is different, so that the validation will always validate on your new metric.

an example:

```yaml
receivers:
  awsecscontainermetrics:
exporters:
  logging:
    loglevel: debug
  awsemf:
    namespace: '${otel_service_namespace}/${otel_service_name}'
    region: '${region}'

service:
  pipelines:
    metrics:
      receivers: [awsecscontainermetrics]
      exporters: [logging, awsemf]
```

#### 3.2 ecs task definition 

Below are the placeholders you can use in the ecs task def.

* region
* aoc_image
* data_emitter_image
* testing_id
* otel_service_namespace
* otel_service_name
* ssm_parameter_arn
* sample_app_container_name
* sample_app_listen_address

an example:

```json
[
    {
      "name": "${sample_app_container_name}",
      "image": "${data_emitter_image}",
      "cpu": 10,
      "memory": 256,
      "portMappings": [
          {
            "containerPort": 4567,
            "hostPort": 4567,
            "protocol": "tcp"
          }
      ],
      "command": [],
      "environment": [
        {
          "name": "OTEL_EXPORTER_OTLP_ENDPOINT",
          "value": "127.0.0.1:55680"
        },
        {
          "name": "INSTANCE_ID",
          "value": "${testing_id}"
        },
        {
        "name": "OTEL_RESOURCE_ATTRIBUTES",
        "value": "service.namespace=${otel_service_namespace},service.name=${otel_service_name}"
        },
        {
        "name": "S3_REGION",
        "value": "${region}"
        },
        {
        "name": "TRACE_DATA_BUCKET",
        "value": "trace-expected-data"
        },
        {
        "name": "TRACE_DATA_S3_KEY",
        "value": "${testing_id}"
        },
        {
            "name": "LISTEN_ADDRESS",
            "value": "${sample_app_listen_address}"
        }
      ],
      "dependsOn": [
        {
            "containerName": "aoc-collector",
            "condition": "START"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ecs-cwagent-sidecar-emitter",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "True"
        }
      }
    },
    {
      "name": "aoc-collector",
      "image": "${aoc_image}",
      "cpu": 10,
      "memory": 256,
      "portMappings": [
        {
          "containerPort": 55680,
          "hostPort": 55680,
          "protocol": "tcp"
        }
      ],
      "secrets": [
        {
            "name": "AOT_CONFIG_CONTENT",
            "valueFrom": "${ssm_parameter_arn}"
        }
      ],
      "essential": true,
      "entryPoint": [],
      "command": [],
      "environment": [],
      "environmentFiles": [],
      "dependsOn": [],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ecs-cwagent-sidecar-collector",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "True"
        }
      }
    }
]
```

#### 3.3 Docker compose file

Below are the placeholders you can use in the docker compose file

* data_emitter_image
* sample_app_listen_address_port
* listen_address
* testing_id
* otel_resource_attributes
* otel_endpoint

an example:

```yaml
version: "3.8"
services:
  sample_app:
    image: ${data_emitter_image}
    ports:
      - "80:${sample_app_listen_address_port}"
    environment:
      LISTEN_ADDRESS: ${listen_address}
      OTEL_RESOURCE_ATTRIBUTES: ${otel_resource_attributes}
      INSTANCE_ID: ${testing_id}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${otel_endpoint}
    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:${sample_app_listen_address_port}/"]
      interval: 5s
      timeout: 10s
      retries: 3
      start_period: 10s
```

#### 3.4 Eks Config

Below are the placeholders you can use in the EKS config.

* data_emitter_image
* testing_id

an example:

```yaml
sample_app:
  image: ${data_emitter_image}
  command:
    - "/bin/sh"
    - "-c"
    - "while true; do echo 'testCounter.metric_${testing_id}:1.7|c|@0.1|#key:val,key1:val1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; echo 'testGauge.metric_${testing_id}:1.8|c|@0.1|#keyg:valg,keyg1:valg1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; done"
  args: []
```

#### 3.5 Validation config.

An example for validation config.

```yaml
-
  validationType: "trace"
  httpPath: "/span0"
  httpMethod: "get"
  callingType: "http"
  expectedTraceTemplate: "DEFAULT_EXPECTED_TRACE"
-
  validationType: "cw-metric"
  httpPath: "/span0"
  httpMethod: "get"
  callingType: "http"
  expectedMetricTemplate: "DEFAULT_EXPECTED_METRIC"
```

the validation config is a list of validation, below are the explanation for each field.

* validationType, possible value: `metric` which validates metric from Cloudwatch, `trace` which validates traces from Xray.
* callingType, possible value: `http` which will ask validator to call the sample app with http protocol. `none`, which tell the validator the metric or trace will be automatically generated from the sample app/AOC. 
* httpPath: the path to call, only apply when callingType is `http`
* httpMethod: the http method to call, only apply when callingType is `http`
* expectedTraceTemplate: the trace validation pattern, only apply when validationType is `trace`
* expectedMetricTemplate: the metric validation pattern, only apply when validationType is `metric`

Below are the placeholders you can use in the expected data pattern.

* metricNamespace
* testingId
* ecsContext.ecsClusterName
* ecsContext.ecsTaskArn
* ecsContext.ecsTaskDefFamily
* ecsContext.ecsTaskDefVersion

an example: 

```yaml
-
  metricName: latency_{{testingId}}
  namespace: {{metricNamespace}}
  dimensions:
    -
      name: OTelLib
      value: cloudwatch-otel
    -
      name: apiName
      value: /span0
    -
      name: statusCode
      value: 200
-
  metricName: latency_{{testingId}}
  namespace: {{metricNamespace}}
  dimensions:
    -
      name: OTelLib
      value: cloudwatch-otel
    -
      name: apiName
      value: /span1
    -
      name: statusCode
      value: 200
-
  metricName: latency_{{testingId}}
  namespace: {{metricNamespace}}
  dimensions:
    -
      name: OTelLib
      value: cloudwatch-otel
    -
      name: apiName
      value: /span2
    -
      name: statusCode
      value: 200
-
  metricName: latency_{{testingId}}
  namespace: {{metricNamespace}}
  dimensions:
    -
      name: OTelLib
      value: cloudwatch-otel
    -
      name: apiName
      value: /
    -
      name: statusCode
      value: 200
```


## 4. Build Sample App

For any testing suite related with sdk, you are required to build a sample app. 

below is the requirement for sample app.

* Web application/Docker image: *The data emitter needs to be a web application serving a http endpoint, and needs to be able to built as a Docker image and run for testing as a Docker app. A Dockerfile needs to be provided.

* Integrate with SDK Repo workflow: *For each new code commit you make in the SDK, you will need to build a new Docker image. Every new Docker image should have a unique tag (you can use the code commit as the tag). Also maintain a “latest” tag for this image which builds with the released SDK. This will help theAOC (https://github.com/aws-observability/aws-otel-collector) repo to use the latest tag image to test with the latest version of the SDK. The SDK workflow sends a dispatch event with the built image link.

* Response pattern: The response of each URL path need to be followed using the data structure listed below i.e. you just need to return the traceid. [json/application]
```json
{
"traceId": "xxxx"
}
```

* Unique ID. [skip it if you just do trace not metric]Every time the validator, which is a component in the testing framework, calls the URL, the validator will generate a test-suite-id, which is unique for each run. The URL parameter, for example: http://x.x.x.x:8080/get-trace?testing-id=12313123241, can be used as an unique identifier for the metric e.g. adding this id as part of the metric name so that the metric could be different for each testing run. Note that no matter which http method you use, it will not be in the body, but just in the url parameter.
Environment Variable: There will be some env vars which will be set while running this web application. The env vars will be but not limited to:
    1. OTEL_EXPORTER_OTLP_ENDPOINT
    2. OTEL_RESOURCE_ATTRIBUTES
    3. LISTEN_ADDRESS : *This is a mandatory environment variable. This web application address, for example (0.0.0.0:8080), makes the address controllable by the upper level; prevents port conflict with any other software on the testbed and also helps us limit the web application access within a private subnet.
    
* Support multiple URL path: *There’s no limitation on the URL path. There could be multiple URLs in this web application depending on the number of test cases you want.

* Keep “/” accessible with response code 200.: This “/” will be used for the load balancer health check.

## 5. configure the testing suite in github workflow

below is an example in the aoc repo workflow, you can also configure it in your sdk repos.

```shell
  e2etest-ecs:
    strategy:
      matrix:
        type: [EC2, FARGATE]
    runs-on: ubuntu-latest
    needs: [e2etest-release, e2etest-preparation]
    steps:
      - name: Configure AWS Credentials
        uses: aws-actions/configure-aws-credentials@v1
        with:
          aws-access-key-id: ${{ secrets.INTEG_TEST_AWS_KEY_ID }}
          aws-secret-access-key: ${{ secrets.INTEG_TEST_AWS_KEY_SECRET }}
          aws-region: us-west-2
          
      - name: Set up JDK 1.11
        uses: actions/setup-java@v1
        with:
          java-version: 1.11
      
      - name: Set up terraform
        uses: hashicorp/setup-terraform@v1
      
      - name: Check out testing framework
        uses: actions/checkout@v2
        with:
          repository: 'aws-observability/aws-otel-collector-test-framework'
          ref: 'terraform'

      - name: Run testing suite
        run: |
          cd terraform/ecs && terraform init && terraform apply -auto-approve -var="ecs_launch_type=${{ matrix.type }}" -var="aoc_version=${{ needs.e2etest-preparation.outputs.version }}"
          
      - name: Destroy resources
        if: ${{ always() }}
        run: |
          cd terraform/ecs && terraform destroy -auto-approve 
```
