#!/bin/bash

./gradlew run --args="\
--runner='DataflowRunner' \
--project=${PROJECT} \
--region=${REGION} \
--subnetwork=${SUBNETWORK} \
--tempLocation=${TEMP_LOCATION} \
--serviceAccount=${SERVICE_ACCOUNT} \
--usePublicIps=${DATAFLOW_VM_PUBLIC_IP}"

