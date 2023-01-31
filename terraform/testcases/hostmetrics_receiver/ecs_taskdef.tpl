[
    {
      "name": "aoc-collector",
      "image": "${aoc_image}",
      "cpu": 10,
      "memory": 256,
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
          "awslogs-group": "/ecs/aoc-collector",
          "awslogs-region": "${region}",
          "awslogs-stream-prefix": "ecs",
          "awslogs-create-group": "True"
        }
      }
    }
]