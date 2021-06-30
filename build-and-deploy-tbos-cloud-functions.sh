PROJECT_ID=$1

echo "Will deploy to ${PROJECT_ID} "

mvn -Pcloud-function -DskipTests clean install package


echo "Deploying testboston kit dispatcher to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    tbos-kit-dispatcher \
    --entry-point=org.broadinstitute.dsm.cf.TestBostonKitOrderer \
    --runtime=java11 \
    --trigger-topic=tbos-kit-dispatcher \
    --timeout=600 \
    --memory=1Gib \
    --source=target/deployment \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=study-manager-config,DDP_INSTANCE=testboston,EMAIL_TO=testboston-notifications@broadinstitute.org,EMAIL_FROM=notifications-noreply@datadonationplatform.org,SHIPPING_RATE=3rd Day Air Residential" \
    --vpc-connector=projects/${PROJECT_ID}/locations/us-central1/connectors/appengine-default-connect




echo "Deploying participant status report to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    tbos-participant-report \
    --entry-point=org.broadinstitute.dsm.cf.ParticipantStatusReport \
    --runtime=java11 \
    --trigger-topic=tbos-participant-status \
    --timeout=600 \
    --memory=1Gib \
    --source=target/deployment \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=study-manager-config,DDP_INSTANCE=testboston,REPORT_BUCKET=,REPORT_PATH \
    --vpc-connector=projects/${PROJECT_ID}/locations/us-central1/connectors/appengine-default-connect

