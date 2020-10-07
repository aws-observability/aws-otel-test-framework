[
    {
      "name": "aoc-emitter",
      "image": "alpine/socat:latest",
      "cpu": 10,
      "memory": 256,
      "entryPoint": [
        "/bin/sh",
        "-c",
        "while true; do echo 'testCounter.metric_${testing_id}:1.7|c|@0.1|#key:val,key1:val1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; echo 'testGauge.metric_${testing_id}:1.8|c|@0.1|#keyg:valg,keyg1:valg1' | socat -v -t 0 - UDP:127.0.0.1:8125; sleep 1; done"
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
      "image": "gavindoudou/aocrepo:v0.1.10",
      "cpu": 10,
      "memory": 256,
      "portMappings": [
        {
          "containerPort": 8125,
          "hostPort": 8125,
          "protocol": "udp"
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