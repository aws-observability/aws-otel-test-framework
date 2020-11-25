version: '3.8'

services:
  validator:
    build:
      ../../validator
    volumes:
      - ~/.aws:/root/.aws
      - ./output:/var/output
    env_file:
      - creds.env
    command:
      - "-c=${validation_config}"
      - "-t=${testing_id}"
      - "--region=${region}"
      - "--endpoint=${sample_app_endpoint}"
      - "--mocked-server-validating-url=${mocked_server_validating_url}"
      - "--metric-namespace=${metric_namespace}"
      - "--ecs-context"
      - "ecsClusterName=${ecs_cluster_name}"
      - "--ecs-context"
      - "ecsTaskArn=${ecs_task_arn}"
      - "--ecs-context"
      - "ecsTaskDefFamily=${ecs_taskdef_family}"
      - "--ecs-context"
      - "ecsTaskDefVersion=${ecs_taskdef_version}"
      - "--alarm-names=${cpu_alarm}"
      - "--alarm-names=${mem_alarm}"
      - "--alarm-names=${incoming_packets_alarm}"

