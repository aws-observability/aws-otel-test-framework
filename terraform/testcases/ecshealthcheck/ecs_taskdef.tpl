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
          "name": "INSTANCE_ID",
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
      },
      "healthCheck": {
        "command": [
          "/healthcheck"
        ],
        "interval": 5,
        "timeout": 6,
        "retries": 5,
        "startPeriod": 1
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
