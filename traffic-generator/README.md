# This is a minimal alpine image

## Requirements 
* Run bash commands
* Run curl
* Container started with bash command

## Build the container with amd and arm images

docker buildx build --push --platform=linux/amd64,linux/arm64 -t 611364707713.dkr.ecr.us-west-2.amazonaws.com/otel-test/container-insight-samples:traffic-generator -f Dockerfile .

## Example start command to curl google.com while true

docker run 611364707713.dkr.ecr.us-west-2.amazonaws.com/otel-test/container-insight-samples:traffic-generator "/bin/bash" "-c" "while :; do curl http://google.com; sleep 1s; done"