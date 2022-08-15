[
    {
      "name": "aoc-collector",
      "image": "public.ecr.aws/q1w6y6d0/adotcrossprom:latest",
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
              "readOnly": true,
              "containerPath": "/rootfs/proc",
              "sourceVolume": "proc"
          },
          {
              "readOnly": true,
              "containerPath": "/rootfs/dev",
              "sourceVolume": "dev"
          },
          {
              "readOnly": true,
              "containerPath": "/sys/fs/cgroup",
              "sourceVolume": "al2_cgroup"
          },
          {
              "readOnly": true,
              "containerPath": "/cgroup",
              "sourceVolume": "al1_cgroup"
          },
          {
              "readOnly": true,
              "containerPath": "/rootfs/sys/fs/cgroup",
              "sourceVolume": "al2_cgroup"
          },
          {
              "readOnly": true,
              "containerPath": "/rootfs/cgroup",
              "sourceVolume": "al1_cgroup"
          }
      ],
      "essential": true,
      "entryPoint": [],
      "command": [],
      "environment": [],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/ecs-aoc-daemon-collector",
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
        "startPeriod": 6
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
