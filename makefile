SHELL = '/bin/bash'
REQUIRED_PACKAGES := mutagen mutagen-compose
DEV_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.dev.yml
TEST_COMPOSE_FILES = -f docker/docker-compose.yml -f docker/docker-compose.test.yml
LOCAL_COMPOSE_FILES = -f docker/docker-compose.yml
PROJECT_NAME = hmpps-sentence-plan

export COMPOSE_PROJECT_NAME=${PROJECT_NAME}

default: help

help: ## The help text you're reading.
	@grep --no-filename -E '^[0-9a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-30s\033[0m %s\n", $$1, $$2}'

install_requirements: ## Install any missing required packages outlined in REQUIRED_PACKAGES=
	@for package in $(REQUIRED_PACKAGES); do \
		if ! command -v brew &> /dev/null; then \
			echo "Homebrew is not installed. Please install Homebrew first."; \
			exit 1; \
		fi; \
		if brew list --formula | grep -q "^$${package}$$"; then \
			echo "Package '$${package}' is installed."; \
		else \
			echo "Package '$${package}' is not installed, installing..."; \
			brew install $${package}; \
		fi; \
	done

up: ## Starts/restarts the API in a production container.
	docker compose ${LOCAL_COMPOSE_FILES} down api
	mutagen-compose ${LOCAL_COMPOSE_FILES} up api --wait --no-recreate

down: ## Stops and removes all containers in the project.
	mutagen-compose ${DEV_COMPOSE_FILES} down
	mutagen-compose ${LOCAL_COMPOSE_FILES} down

build-api: ## Builds a production image of the API.
	docker-compose build api

dev-up:install_requirements ## Starts/restarts the API in a development container. A remote debugger can be attached on port 5005.
	docker-compose down api
	mutagen-compose ${DEV_COMPOSE_FILES} up -d

dev-build:install_requirements ## Builds a development image of the API.
	mutagen-compose ${DEV_COMPOSE_FILES} build api

dev-down: ## Stops and removes the API container.
	mutagen-compose down

rebuild: ## Re-builds and live-reloads the API.
	mutagen-compose ${DEV_COMPOSE_FILES} exec api gradle compileKotlin --parallel --build-cache --configuration-cache

watch: ## Watches for file changes and live-reloads the API. To be used in conjunction with dev-up e.g. "make dev-up watch"
	mutagen-compose ${DEV_COMPOSE_FILES} exec api gradle compileKotlin --continuous --parallel --build-cache --configuration-cache

test: ## Runs all the test suites.
	mutagen-compose ${DEV_COMPOSE_FILES} exec api gradle test --parallel

lint: ## Runs the Kotlin linter.
	mutagen-compose ${DEV_COMPOSE_FILES} exec api gradle ktlintCheck --parallel

lint-fix: ## Runs the Kotlin linter and auto-fixes.
	mutagen-compose ${DEV_COMPOSE_FILES} exec api gradle ktlintFormat --parallel

clean: ## Stops and removes all project containers. Deletes local build/cache directories.
	docker compose down
	rm -rf .gradle build

update: ## Downloads the latest versions of containers.
	docker compose pull

save-logs: ## Saves docker container logs in a directory defined by OUTPUT_LOGS_DIR=
	mkdir -p ${OUTPUT_LOGS_DIR}
	docker logs ${PROJECT_NAME}-api-1 > ${OUTPUT_LOGS_DIR}/api.log
