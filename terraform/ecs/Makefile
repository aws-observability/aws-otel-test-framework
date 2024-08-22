# If you are debugging validator but don't want to rebuild the infra, just run
# make build && make validate
# terraform apply generates validator_docker_compose.yml with right parameters

validate:
# You can modify this to whatever temp credential helper you have if your ~/.aws is using credential_process
# The docker compose file mount ~/.aws into the container for getting AWS credential
#	awsl isengard get > creds.env
	docker compose -f validator_docker_compose.yml up

build:
	docker compose -f validator_docker_compose.yml build
