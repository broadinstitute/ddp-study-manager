
PROJECT_ID=$1
SECRET_ID=$2
STUDY_MANAGER_SCHEMA=$3
STUDY_SERVER_SCHEMA=$4

echo "Will deploy to ${PROJECT_ID} with ${SECRET_ID} secret using schemas ${STUDY_MANAGER_SCHEMA} and ${STUDY_SERVER_SCHEMA}"

mvn -Pcloud-function -DskipTests clean install package

echo "Deploying to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    tbos-kit-tracking-dispatcher \
    --entry-point=org.broadinstitute.dsm.cf.TestBostonKitTrackerDispatcher \
    --runtime=java11 \
    --trigger-topic=kit-report \
    --source=target/deployment \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=${SECRET_ID},STUDY_MANAGER_SCHEMA=${STUDY_MANAGER_SCHEMA},STUDY_SERVER_SCHEMA=${STUDY_SERVER_SCHEMA}"