#!/usr/bin/env bash
set -eu -o pipefail

PROJECT_ID=$1

echo "Reading ellkay config from cloud secret manager"
CONFIG_SECRETS=ellkay
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="${CONFIG_SECRETS}" > config/ellkay.conf

echo "Reading test-config from cloud secret manager"
CONFIG_SECRETS=dsm-test-config
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="${CONFIG_SECRETS}" > config/test-config.conf