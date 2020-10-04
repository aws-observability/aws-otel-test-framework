[
    {
      "name": "aoc-emitter",
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
      "essential": true,
      "command": [],
      "environment": [
        {
          "name": "OTEL_EXPORTER_OTLP_ENDPOINT",
          "value": "172.17.0.1:55680"
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
        }
      ],
      "dependsOn": [],
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
            "name": "AOC_CONFIG_CONTENT",
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