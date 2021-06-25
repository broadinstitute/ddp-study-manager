#!/usr/bin/env bash
set -eu -o pipefail

NAME=$(basename "$0")
if (( $# < 2 )); then
  echo "usage: $NAME <PROJECT_ID> <SECRET_ID>"
  exit 1
fi

PROJECT_ID=$1
CONFIG_SECRETS=$2

echo "=> rendering yaml file"
cat StudyManager.tmpl.yaml \
  | sed "s/{{project_id}}/$PROJECT_ID/g" \
  > StudyManager.yaml

echo "=> reading configs from cloud secret manager"
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="${CONFIG_SECRETS}" > vault.conf

echo "=> running build"
mvn -Pbackend -DskipTests clean install package -f ../../pom.xml

# bundling dependencies
rm -fr lib
mkdir -p lib
mvn -Papis -f ../../pom.xml dependency:copy-dependencies -DoutputDirectory=./appengine/deploy/lib
cp ../../target/DSMServer.jar .
cp ../../src/main/resources/log4j.xml .

echo "=> downloading and configuring tcell"
gsutil cat gs://ddp-tcell/tcell-1.11.0.tar.gz | tar -xf -
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="study-manager-tcell" > tcell/tcell_agent.config

echo "=> deploying to appengine"
gcloud --project=${PROJECT_ID} app deploy -q --stop-previous-version --promote StudyManager.yaml

echo "Deleting older versions"
CONFIG_FILE="StudyManager.yaml"
SERVICE=$(grep -Eo -m1 "^service:\s*.*" "${CONFIG_FILE}" | awk '{print $2}')
            echo "The service name is *${SERVICE}*"
VERSIONS=$(gcloud app versions list --service  "${SERVICE}" --project ${PROJECT_ID} --sort-by '~version' --format 'value(version.id)')
echo $VERSIONS
echo "Will keep the latest 3 versions"
COUNT=0
for VERSION in $VERSIONS
do
    ((COUNT++))
    if [ $COUNT -gt 3 ]
    then
      echo "Going to delete version $VERSION of the ${SERVICE} service."
      gcloud app versions delete --quiet $VERSION --service ${SERVICE} --project ${PROJECT_ID} -q
    else
      echo "Going to keep version $VERSION of the ${SERVICE} service."
    fi
done

