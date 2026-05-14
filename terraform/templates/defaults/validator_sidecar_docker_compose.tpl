services:
  validator:
    build:
      /tmp/validator
    network_mode: "host"
    environment:
      - AWS_REGION=${region}
      - AWS_DEFAULT_REGION=${region}
    command:
      - "-c=${validation_config}"
      - "-t=${testing_id}"
      - "--account-id=${account_id}"
      - "--language=${language}"
      - "--region=${region}"
      - "--availability-zone=${availability_zone}"
      - "--endpoint=http://localhost:${sample_app_port}"
      - "--mocked-server-validating-url=http://localhost:80/check-data"
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
      - "--kubecfg-file-path="
      - "--k8s-deployment-name="
      - "--k8s-namespace="
