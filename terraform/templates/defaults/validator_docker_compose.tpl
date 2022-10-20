version: '3.8'

services:
  validator:
    build:
      ../../validator
    volumes:
      - ~/.aws:/root/.aws
      - ./output:/var/output
    environment:
      - AWS_ACCESS_KEY_ID
      - AWS_SECRET_ACCESS_KEY
      - AWS_SESSION_TOKEN
      - AWS_REGION
      - AWS_DEFAULT_REGION
    command:
      - "-c=${validation_config}"
      - "-t=${testing_id}"
      - "--account-id=${account_id}"
      - "--region=${region}"
      - "--availability-zone=${availability_zone}"
      - "--endpoint=${sample_app_endpoint}"
      - "--mocked-server-validating-url=${mocked_server_validating_url}"
      - "--metric-namespace=${metric_namespace}"
      - "--canary=${canary}"
      - "--testcase=${testcase}"
      - "--cloudwatch-context=${cloudwatch_context_json}"
      - "--ecs-context=${ecs_context_json}"
      - "--ec2-context=${ec2_context_json}"
      - "--alarm-names=${cpu_alarm}"
      - "--alarm-names=${mem_alarm}"
      - "--alarm-names=${incoming_packets_alarm}"
      - "--cortex-instance-endpoint=${cortex_instance_endpoint}"
      - "--rollup=${rollup}"
