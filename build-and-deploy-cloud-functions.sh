PROJECT_ID=$1
STUDY_MANAGER_SCHEMA=$2

echo "Will deploy to ${PROJECT_ID} using schemas ${STUDY_MANAGER_SCHEMA} "

 mvn -Pcloud-function -DskipTests clean install package


echo "Deploying kit dispatcher to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    tbos-kit-tracking-id-finder \
    --entry-point=org.broadinstitute.dsm.cf.TestBostonKitTrackerDispatcher \
    --runtime=java11 \
    --trigger-topic=cron-topic \
    --timeout=540\
    --source=target/deployment \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=cf-kit-tracker,STUDY_MANAGER_SCHEMA=${STUDY_MANAGER_SCHEMA}" \
    --vpc-connector=projects/${PROJECT_ID}/locations/us-central1/connectors/appengine-default-connect


echo "Deploying kit boston tracking to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    tbos-kit-ups-tracker \
    --entry-point=org.broadinstitute.dsm.jobs.TestBostonUPSTrackingJob \
    --runtime=java11 \
    --trigger-topic=cf-kit-tracking \
    --timeout=540\
    --source=target/deployment \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=cf-kit-tracker,STUDY_MANAGER_SCHEMA=${STUDY_MANAGER_SCHEMA}" \
    --vpc-connector=projects/${PROJECT_ID}/locations/us-central1/connectors/appengine-default-connect




#echo "Deploying covid order registrar to ${PROJECT_ID}"
#gcloud --project=${PROJECT_ID} functions deploy \
#    order-in-care-evolve \
#    --entry-point=org.broadinstitute.dsm.cf.Covid19OrderRegistrarFunction \
#    --runtime=java11 \
#    --trigger-topic=tbos-ce-orders \
#    --source=target/deployment \
#    --memory=1024MB \
#    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=study-manager-config" \
#    --vpc-connector=projects/broad-ddp-dev/locations/us-central1/connectors/appengine-default-connect
