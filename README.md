# HMPPS Sentence Plan API
[![repo standards badge](https://img.shields.io/badge/dynamic/json?color=blue&style=flat&logo=github&label=MoJ%20Compliant&query=%24.result&url=https%3A%2F%2Foperations-engineering-reports.cloud-platform.service.justice.gov.uk%2Fapi%2Fv1%2Fcompliant_public_repositories%2Fhmpps-sentence-plan)](https://operations-engineering-reports.cloud-platform.service.justice.gov.uk/public-github-repositories.html#hmpps-sentence-plan "Link to report")
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-sentence-plan/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-sentence-plan)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-sentence-plan/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-sentence-plan)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://hmpps-sentence-plan-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

This is the backend/API for the Sentence Plan project, which aims to enhance the current 
process of creating and managing sentence plans for individuals on probation or in prison.

## Prerequisites
- Docker

## Running the application
This service and all of its dependencies are run in Docker containers.

**Note:** Every command can be printed using `make`

**Note:** An access token (for authenticating with the API) can be generated
by performing an OAuth2 client_credentials grant flow to http://localhost:9091/auth/oauth/token, using
`sentence-plan-api-client` for both the client_id and client_secret

### Production
1. To start a production version of the application, run `make up`
    - The service will start on http://localhost:8080
    - To check the health status, go to http://localhost:8080/health
2. To update all containers, run `make down update up`

### Development
1. To start a development version of the application, run `make dev-up`
    - The service will start on http://localhost:8080
    - A debugger session will be accessible on http://localhost:5005
    - To check the health status, go to http://localhost:8080/health
2. To enable live-reload, run `make watch`, the API will now restart each time you change the code.

You can connect to the remote debugger session on http://localhost:5005 like so
[![API docs](https://github.com/ministryofjustice/hmpps-strengths-based-needs-assessments-api/blob/main/.readme/debugger.png?raw=true)]()

### Testing
The test suite can be ran using `make test`

### Linting
Linting can be ran using `make lint` and `make lint-fix`
