#!/bin/bash

./gradlew run --args="\
--runner='DataflowRunner' \
--project=${PROJECT} \
--region=${REGION} \
--subnetwork=${SUBNETWORK} \
--tempLocation=${TEMP_LOCATION} \
--serviceAccount=${SERVICE_ACCOUNT} \
--usePublicIps=${DATAFLOW_VM_PUBLIC_IP} \
--alloydbDatabase=${ALLOYDB_DB_NAME} \
--alloydbUser=${ALLOYDB_DB_USER} \
--alloydbPassword=${ALLOYDB_DB_PASSWORD} \
--alloydbInstance=${ALLOYDB_DB_INSTANCE} \
--numWorkers=${MAX_DATAFLOW_WORKERS} \
--diskSizeGb=${DISK_SIZE_GB} \
--workerMachineType=${MACHINE_TYPE} \
--sdkHarnessLogLevelOverrides='{\"com.zaxxer.hikari\":\"DEBUG\"}' \
--autoscalingAlgorithm=NONE "

