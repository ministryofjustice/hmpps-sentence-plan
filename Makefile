SHELL = '/bin/bash'
DEV_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.local.yml -f docker/docker-compose.dev.yml
LOCAL_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.local.yml
PROJECT_NAME = hmpps-assess-risks-and-needs

export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

up: ## Starts/restarts the API in a production container.
	docker compose ${LOCAL_COMPOSE_FILES} down sp-api
	docker compose ${LOCAL_COMPOSE_FILES} up sp-api --wait --no-recreate

down: ## Stops and removes all containers in the project.
	docker compose ${DEV_COMPOSE_FILES} down
	docker compose ${LOCAL_COMPOSE_FILES} down

build-api: ## Builds a production image of the API.
	docker compose ${LOCAL_COMPOSE_FILES} build sp-api

dev-up: ## Starts/restarts the API in a development container. A remote debugger can be attached on port 5005.
	docker compose ${DEV_COMPOSE_FILES} down sp-api
	docker compose ${DEV_COMPOSE_FILES} up --wait --no-recreate sp-api

dev-build: ## Builds a development image of the API.
	docker compose ${DEV_COMPOSE_FILES} build sp-api

dev-down: ## Stops and removes the API container.
	docker compose ${DEV_COMPOSE_FILES} down sp-api

rebuild: ## Re-builds and reloads the API.
	docker compose ${DEV_COMPOSE_FILES} exec sp-api gradle compileKotlin --parallel --build-cache --configuration-cache

watch: ## Watches for file changes and live-reloads the API. To be used in conjunction with dev-up e.g. "make dev-up watch"
	docker compose ${DEV_COMPOSE_FILES} exec sp-api gradle compileKotlin --continuous --parallel --build-cache --configuration-cache

test: ## Runs all the test suites.
	docker compose ${DEV_COMPOSE_FILES} exec sp-api gradle test --parallel

lint: ## Runs the Kotlin linter.
	docker compose ${DEV_COMPOSE_FILES} exec sp-api gradle ktlintCheck --parallel

lint-fix: ## Runs the Kotlin linter and auto-fixes.
	docker compose ${DEV_COMPOSE_FILES} exec sp-api gradle ktlintFormat --parallel

clean: ## Stops and removes all project containers. Deletes local build/cache directories.
	docker compose down
	docker volume ls -qf "dangling=true" | xargs -r docker volume rm
	rm -rf .gradle build

update: ## Downloads the latest versions of containers.
	docker compose ${LOCAL_COMPOSE_FILES} pull

save-logs: ## Saves docker container logs in a directory defined by OUTPUT_LOGS_DIR=
	mkdir -p ${OUTPUT_LOGS_DIR}
	docker logs ${PROJECT_NAME}-sp-api-1 > ${OUTPUT_LOGS_DIR}/sp-api.log

db-port-forward-pod: ## Creates a DB port-forwarding pod in your currently active Kubernetes context
	kubectl delete pod --ignore-not-found=true port-forward-pod
	INSTANCE_ADDRESS=$$(kubectl get secret rds-postgresql-instance-output -o json | jq -r '.data.rds_instance_address' | base64 --decode) \
	; kubectl run port-forward-pod --image=ministryofjustice/port-forward --port=5432 --env="REMOTE_HOST=$$INSTANCE_ADDRESS" --env="LOCAL_PORT=5432" --env="REMOTE_PORT=5432"

DB_PORT_FORWARD_PORT=5434
db-port-forward: ## Forwards port 5434 on your local machine to port 5432 on the port-forwarding pod. Override the local port with DB_PORT_FORWARD_PORT=XXXX
	kubectl wait --for=jsonpath='{.status.phase}'=Running pod/port-forward-pod
	kubectl port-forward port-forward-pod ${DB_PORT_FORWARD_PORT}:5432

db-connection-string: ## Outputs a DB connection string that will let you connect to the remote DB through the port-forwarding pod. Override the local port with DB_PORT_FORWARD_PORT=XXXX
	@DATABASE_USERNAME=$$(kubectl get secret rds-postgresql-instance-output -o json | jq -r '.data.database_username' | base64 --decode) \
	DATABASE_PASSWORD=$$(kubectl get secret rds-postgresql-instance-output -o json | jq -r '.data.database_password' | base64 --decode) \
	DATABASE_NAME=$$(kubectl get secret rds-postgresql-instance-output -o json | jq -r '.data.database_name' | base64 --decode) \
	; echo postgres://$$DATABASE_USERNAME:$$DATABASE_PASSWORD@localhost:${DB_PORT_FORWARD_PORT}/$$DATABASE_NAME

db-connect: ## Connects to the remote DB though the port-forwarding pod
	psql --pset=pager=off $$(make db-connection-string)
