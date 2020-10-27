[
    {
      "name": "${sample_app_container_name}",
      "image": "${data_emitter_image}",
      "cpu": 10,
      "memory": 256,
      "portMappings": [
          {
            "containerPort": ${sample_app_listen_port},
            "hostPort": ${sample_app_listen_port},
            "protocol": "tcp"
          }
      ],
      "command": [],
      "environment": [
        {
            "name": "LISTEN_ADDRESS",
            "value": "${sample_app_listen_address}"
        },
        {
            "name": "AWS_XRAY_DAEMON_ADDRESS",
            "value": "127.0.0.1:2000"
        },
        {
            "name": "AWS_REGION",
            "value": "${region}"
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
          "containerPort": 2000,
          "hostPort": 2000,
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