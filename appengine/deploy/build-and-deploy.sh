#!/usr/bin/env bash
set -eu -o pipefail

PROJECT_ID=$1

echo "Reading configs from cloud secret manager"
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="study-manager-config" > vault.conf

#  run the build
echo "Running maven"
mvn -DskipTests clean install package -f ../../pom.xml

# bundling dependencies
rm -fr lib
mkdir -p lib
mvn -f ../../pom.xml dependency:copy-dependencies -DoutputDirectory=./appengine/deploy/lib
cp ../../target/dsm-backend-SNAPSHOT.jar .

echo "Downloading and configuring tcell"
gsutil cat gs://ddp-tcell/tcell-1.11.0.tar.gz | tar -xf -
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="study-manager-tcell" >  tcell/tcell_agent.config

# deploy to gae
gcloud app deploy -q --stop-previous-version --project ${PROJECT_ID} StudyManager.yaml
