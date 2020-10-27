version: "3.8"
services:
  sample_app:
    image: ${data_emitter_image}
    ports:
      - "${sample_app_external_port}:${sample_app_listen_address_port}"
    environment:
      LISTEN_ADDRESS: ${listen_address}
      OTEL_RESOURCE_ATTRIBUTES: ${otel_resource_attributes}
      INSTANCE_ID: ${testing_id}
      OTEL_EXPORTER_OTLP_ENDPOINT: ${otel_endpoint}
      AWS_REGION: ${region}

    healthcheck:
      test: ["CMD", "curl", "-f", "http://127.0.0.1:${sample_app_listen_address_port}/"]
      interval: 5s
      timeout: 10s
      retries: 3
      start_period: 10s