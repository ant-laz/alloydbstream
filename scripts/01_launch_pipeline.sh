./gradlew run --args="\
--runner='DataflowRunner' \
--project=${GCP_PROJECT_ID} \
--serviceAccount=${SERVICE_ACCT} \
--region=${GCP_DATAFLOW_REGION} \
--tempLocation=${GCS_BUCKET_TMP} \
--network=${NETWORK_NAME} \
--subnetwork=${SUBNETWORK_NAME} \
--usePublicIps=${PUBLICIP_FLAG} \
--targetDatabase${ADB_DB}"

