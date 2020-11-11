version: "3.8"
services:
  ot-metric-emitter:
    image: ${data_emitter_image}
    command: ["${date_mode}", "-r=${rate}", "-u=${grpc_endpoint}", "-d=${data_type}"]
    environment:
      OTEL_RESOURCE_ATTRIBUTES: ${otel_resource_attributes}
      AWS_XRAY_DAEMON_ADDRESS: ${udp_endpoint}
      AWS_REGION: ${region}