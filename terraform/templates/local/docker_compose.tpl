version: "3.8"
services:
  python-server:
    image: python
    working_dir: "/app"
    command: ["/app/start-server.sh"]
    environment:
      - LISTEN_ADDRESS=0.0.0.0:5678
    volumes:
      - ../../mockserver:/app

  mocked-server:
    image: nginx:latest
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
      - ../../mockserver/cert.pem:/etc/cert.pem
      - ../../mockserver/key.pem:/etc/key.pem
    ports:
      - 80:80

  aws-ot-collector:
    build:
      context: ../../../aws-otel-collector
      dockerfile: cmd/awscollector/Dockerfile

    command: ["--config=/tmp/otconfig.yaml", "--log-level=DEBUG"]
    volumes:
      - ./otconfig.yml:/tmp/otconfig.yaml
      - "../../mockserver/cert.pem:/etc/ssl/certs/ca-certificates.crt"
      - "../../mockserver/cert.pem:/etc/ssl/cert.pem"
      - "../../mockserver/cert.pem:/etc/pki/tls/certs/ca-bundle.crt"
      - "../../mockserver/cert.pem:/etc/ssl/ca-bundle.pem"
      - "../../mockserver/cert.pem:/etc/pki/tls/cacert.pem"
      - "../../mockserver/cert.pem:/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
    environment:
      - AWS_REGION=${region}
      - GODEBUG=x509ignoreCN=0
    volumes:
      - ~/.aws:/root/.aws

  sample_app:
    image: ${data_emitter_image}
    ports:
      - "${sample_app_external_port}:${sample_app_listen_address_port}"
    environment:
      - LISTEN_ADDRESS=${sample_app_listen_address}
      - AWS_REGION=${region}
      - OTEL_RESOURCE_ATTRIBUTES=${otel_resource_attributes}
      - INSTANCE_ID=${testing_id}
      - OTEL_EXPORTER_OTLP_ENDPOINT=aws-ot-collector:${grpc_port}
      - AWS_XRAY_DAEMON_ADDRESS=aws-ot-collector:${udp_port}
