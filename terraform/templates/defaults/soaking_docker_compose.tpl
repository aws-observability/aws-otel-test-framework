version: "3.8"
services:
  mocked_server:
    image: ${mocked_server_image}
    ports:
      - "80:8080"
      - "443:443"
  ot-metric-emitter:
    image: ${sample_app_image}
    command: ["${data_mode}", "-r=${rate}", "-u=${grpc_endpoint}", "-d=${data_type}"]
    environment:
      OTEL_RESOURCE_ATTRIBUTES: ${otel_resource_attributes}
      AWS_XRAY_DAEMON_ADDRESS: ${udp_endpoint}
      AWS_REGION: ${region}