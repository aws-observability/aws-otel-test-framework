version: "3.8"
services:
  mocked_server:
    image: ${mocked_server_image}
    ports:
      - "80:8080"
      - "443:443"
    deploy:
      resources:
        limits:
          memory: 1G
  ot-metric-emitter:
    privileged: true
    image: ${sample_app_image}
    command: []
    ports:
      - ${sample_app_external_port}:${sample_app_listen_address_port}
    environment:
      METRICS_LOAD: ${rate}
      LISTEN_ADDRESS: ${listen_address}
    deploy:
      resources:
        limits:
          memory: 16G
