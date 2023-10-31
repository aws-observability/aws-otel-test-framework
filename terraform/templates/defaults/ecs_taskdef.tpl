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
          "value": "http://127.0.0.1:${grpc_port}"
        },
        {
           "name": "AWS_XRAY_DAEMON_ADDRESS",
           "value": "127.0.0.1:${udp_port}"
        },
        {
           "name": "COLLECTOR_UDP_ADDRESS",
           "value": "127.0.0.1:${udp_port}"
        },
        {
            "name": "AWS_REGION",
            "value": "${region}"
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
            "name": "LISTEN_ADDRESS",
            "value": "${sample_app_listen_address}"
        },
        {
            "name": "JAEGER_RECEIVER_ENDPOINT",
            "value": "127.0.0.1:${http_port}"
        },
        {
            "name": "ZIPKIN_RECEIVER_ENDPOINT",
            "value": "127.0.0.1:${http_port}"
        },
        {
            "name": "OTEL_LOGS_EXPORTER",
            "value": "otlp"
        },
        {
            "name": "SAMPLE_APP_LOG_LEVEL",
            "value": "INFO"
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
          "containerPort": 4317,
          "hostPort": 4317,
          "protocol": "tcp"
        },
        {
           "containerPort": 2000,
           "hostPort": 2000
        }
      ],
      "secrets": [
        {
            "name": "AOT_CONFIG_CONTENT",
            "valueFrom": "${ssm_parameter_arn}"
        }
      ],
      "mountPoints": [
          {
              "sourceVolume": "efs",
              "containerPath": "/etc/pki/tls/certs"
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
    },
    {
      "name": "mocked-server",
      "image": "${mocked_server_image}",
      "cpu": 10,
      "memory": 256,
      "portMappings": [
           {
             "containerPort": 8080,
             "hostPort": 8080,
             "protocol": "tcp"
           }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/collector-mocked-server",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "True"
        }
      }
    }
]