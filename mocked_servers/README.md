# Mocked Servers

Most of the tests use mocked servers as stand-ins for the exporters to send to.
The docker images are built and pushed to ECR as part of `terraform/imagebuild` and then used in the tests as part of `defaults/docker_compose.tpl`.

Most will use the `https` mocked_server, but some test cases like `otlp_grpc_exporter_metric_mock` and `otlp_grpc_exporter_trace_mock` will need to use the `grpc_metrics` and `grpc_trace` mocked_servers.

The servers themselves are very basic. They listen for a message and tend to set a flag to success upon receiving it. Typically, they are set up with two servers: one to mock the backend and one to report on the status of that mock (if endpoint is called, how many times, etc.). There's a built-in 15ms of latency between each message that's received. The status reporter will always be on port 8080 and support the `:8080/` (returns "healthcheck") and the `:8080/check-data` (returns "success" if mock has received messages) endpoints.