# This is a minimal alpine image

## Requirements 
* Run bash commands
* Run curl
* Container started with bash command

## Automated build

The image is rebuilt and pushed by the `Update Traffic Generator Image` workflow
(`.github/workflows/update-traffic-generator-image.yml`) every Monday and whenever this directory changes on the
`terraform` branch, so it picks up Alpine security updates. The base image is pinned to an Alpine release series in the
`Dockerfile`; bump it before that series reaches end of life. The manual command below still works for ad-hoc builds.

## Build the container with amd and arm images

docker buildx build --push --platform=linux/amd64,linux/arm64 -t 611364707713.dkr.ecr.us-west-2.amazonaws.com/otel-test/container-insight-samples:traffic-generator -f Dockerfile .

## Example start command to curl google.com while true

docker run 611364707713.dkr.ecr.us-west-2.amazonaws.com/otel-test/container-insight-samples:traffic-generator "/bin/bash" "-c" "while :; do curl http://google.com; sleep 1s; done"