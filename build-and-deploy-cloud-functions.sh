
PROJECT_ID=$1
STUDY_MANAGER_SCHEMA=$3
STUDY_SERVER_SCHEMA=$4

echo "Will deploy to ${PROJECT_ID} using schemas ${STUDY_MANAGER_SCHEMA} and ${STUDY_SERVER_SCHEMA}"

mvn -Pcloud-function -DskipTests clean install package


#echo "Deploying kit dispatcher to ${PROJECT_ID}"
#gcloud --project=${PROJECT_ID} functions deploy \
#    tbos-kit-tracking-dispatcher \
#    --entry-point=org.broadinstitute.dsm.cf.TestBostonKitTrackerDispatcher \
#    --runtime=java11 \
#    --trigger-topic=kit-report \
#    --source=target/deployment \
#    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=cloud-functions,STUDY_MANAGER_SCHEMA=${STUDY_MANAGER_SCHEMA},STUDY_SERVER_SCHEMA=${STUDY_SERVER_SCHEMA}"



echo "Deploying covid order registrar to ${PROJECT_ID}"
gcloud --project=${PROJECT_ID} functions deploy \
    order-in-care-evolve \
    --entry-point=org.broadinstitute.dsm.cf.Covid19OrderRegistrarFunction \
    --runtime=java11 \
    --trigger-topic=tbos-ce-orders \
    --source=target/deployment \
    --memory=1024MB \
    --set-env-vars="PROJECT_ID=${PROJECT_ID},SECRET_ID=study-manager-config"