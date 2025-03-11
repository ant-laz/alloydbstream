## Sink - AlloyDB for PostgreSQL

create environment variables

```shell
source my_config/create_env_vars.sh
```

create a cluster & primary instance by following this guide:

https://cloud.google.com/alloydb/docs/quickstart/create-and-connect#gcloud

create a database 
```shell
CREATE DATABASE automation;
```

create a table
```shell
CREATE TABLE sensors (measurement INTEGER,
                      state VARCHAR(255),
                      sensorID SERIAL PRIMARY KEY);
```

add some initial entries
```shell
INSERT INTO sensors (state, measurement) values ('ACTIVE', 123);
INSERT INTO sensors (state, measurement) values ('ACTIVE', 456);
```

query the table
```shell
SELECT
  sensorid,
  state,
  measurement
FROM
  sensors;
```

| sensorid | state  | measurement |
|----------|--------|-------------|
| 1        | ACTIVE | 123         |
| 2        | ACTIVE | 456         |

manually execute an upsert
```shell
 INSERT INTO sensors (sensorid, state, measurement) 
 VALUES (1, 'ERROR', 789) 
 ON CONFLICT (sensorid) 
 DO UPDATE SET state = EXCLUDED.state, measurement = EXCLUDED.measurement;
```

query the table again
```shell
SELECT
  sensorid,
  state,
  measurement
FROM
  sensors;
```

| sensorid | state  | measurement |
|----------|--------|-------------|
| 1        | ERROR  | 789         |
| 2        | ACTIVE | 456         |



## Dataflow pipeline

create environment variables

```shell
source my_config/create_env_vars.sh
```

navigate to Apache Beam IO

https://beam.apache.org/documentation/io/connectors/

find latest docs for JdbcIO

https://beam.apache.org/releases/javadoc/current/org/apache/beam/sdk/io/jdbc/JdbcIO.html

launch dataflow pipeline

```shell
./gradlew run --args="\
--runner='DataflowRunner' \
--project=${GCP_PROJECT_ID} \
--serviceAccount=${SERVICE_ACCT} \
--region=${GCP_DATAFLOW_REGION} \
--tempLocation=${GCS_BUCKET_TMP} \
--network=${NETWORK_NAME} \
--subnetwork=${SUBNETWORK_NAME} \
--usePublicIps=${PUBLICIP_FLAG} \
--targetDatabase${}
```

