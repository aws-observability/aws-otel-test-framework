version: "3.8"
services:
  mocked-server:
    build:
      context: ../../mocked_servers/${mocked_server}
    ports:
      - 80:8080
      - 55671:55671

  aws-ot-collector:
    build:
      context: ../../../aws-otel-collector
      dockerfile: cmd/awscollector/Dockerfile

    command: ["--config=/tmp/otconfig.yaml", "--log-level=DEBUG"]
    volumes:
      - ./otconfig.yml:/tmp/otconfig.yaml
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/ssl/certs/ca-certificates.crt"
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/ssl/cert.pem"
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/pki/tls/certs/ca-bundle.crt"
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/ssl/ca-bundle.pem"
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/pki/tls/cacert.pem"
      - "../../mocked_servers/https/certificates/ssl/certificate.crt:/etc/pki/ca-trust/extracted/pem/tls-ca-bundle.pem"
    environment:
      - AWS_REGION=${region}
      # faked credentials
      - AWS_ACCESS_KEY_ID=AKIAIOSFODNN7EXAMPLE
      - AWS_SECRET_ACCESS_KEY=wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
      - GODEBUG=x509ignoreCN=0
    depends_on:
      - mocked-server

  sample_app:
    build:
      context: ../../sample-apps/${sample_app}
    ports:
      - "${sample_app_external_port}:${sample_app_listen_address_port}"
    environment:
      - LISTEN_ADDRESS=${sample_app_listen_address}
      - AWS_REGION=${region}
      - OTEL_RESOURCE_ATTRIBUTES=${otel_resource_attributes}
      - INSTANCE_ID=${testing_id}
      - OTEL_EXPORTER_OTLP_ENDPOINT=aws-ot-collector:${grpc_port}
      - AWS_XRAY_DAEMON_ADDRESS=aws-ot-collector:${udp_port}
    depends_on:
      - aws-ot-collector
