#!/usr/bin/env bash

set -eu

POD_NAME="port-forward-pod"
POD_IMAGE="ministryofjustice/port-forward"

SP_NAMESPACE="hmpps-sentence-plan-test"
SP_SECRET_NAME="rds-postgresql-instance-output"

COORDINATOR_NAMESPACE="hmpps-assess-risks-and-needs-integrations-test"
COORDINATOR_SECRET_NAME="hmpps-assess-risks-and-needs-integrations-rds-instance"

SAN_NAMESPACE="hmpps-strengths-based-needs-assessments-test"
SAN_SECRET_NAME="hmpps-strengths-based-needs-assessments-rds-instance"

echo "Fetching secrets..."

SP_INSTANCE_ADDRESS=$(kubectl -n "${SP_NAMESPACE}" get secret "${SP_SECRET_NAME}" -o json | jq -r '.data.rds_instance_address' | base64 --decode)

COORDINATOR_INSTANCE_ADDRESS=$(kubectl -n "${COORDINATOR_NAMESPACE}" get secret "${COORDINATOR_SECRET_NAME}" -o json | jq -r '.data.rds_instance_address' | base64 --decode)

SAN_INSTANCE_ADDRESS=$(kubectl -n "${SAN_NAMESPACE}" get secret "${SAN_SECRET_NAME}" -o json | jq -r '.data.rds_instance_address' | base64 --decode)

echo "Deleting existing port forwarding pods (if any)..."

kubectl -n "${SP_NAMESPACE}" delete pod --ignore-not-found=true "${POD_NAME}"
kubectl -n "${COORDINATOR_NAMESPACE}" delete pod --ignore-not-found=true "${POD_NAME}"
kubectl -n "${SAN_NAMESPACE}" delete pod --ignore-not-found=true "${POD_NAME}"

echo "Starting port-forward pods..."

kubectl -n "${SP_NAMESPACE}" run "${POD_NAME}" \
  --image="${POD_IMAGE}" \
  --port=5432 \
  --env="REMOTE_HOST=${SP_INSTANCE_ADDRESS}" \
  --env="LOCAL_PORT=5432" \
  --env="REMOTE_PORT=5432"

kubectl -n "${COORDINATOR_NAMESPACE}" run "${POD_NAME}" \
  --image="${POD_IMAGE}" \
  --port=5432 \
  --env="REMOTE_HOST=${COORDINATOR_INSTANCE_ADDRESS}" \
  --env="LOCAL_PORT=5432" \
  --env="REMOTE_PORT=5432"

kubectl -n "${SAN_NAMESPACE}" run "${POD_NAME}" \
  --image="${POD_IMAGE}" \
  --port=5432 \
  --env="REMOTE_HOST=${SAN_INSTANCE_ADDRESS}" \
  --env="LOCAL_PORT=5432" \
  --env="REMOTE_PORT=5432"

echo "Pods created."

echo "Waiting for pods to start up..."

kubectl -n "${SP_NAMESPACE}" wait --for=condition=Ready pod/${POD_NAME} --timeout=60s
kubectl -n "${COORDINATOR_NAMESPACE}" wait --for=condition=Ready pod/${POD_NAME} --timeout=60s
kubectl -n "${SAN_NAMESPACE}" wait --for=condition=Ready pod/${POD_NAME} --timeout=60s

echo "Pods ready."
