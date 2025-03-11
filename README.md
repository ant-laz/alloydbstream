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

manually execute an upsert
```shell
 INSERT INTO sensors (sensorid, state, measurement) 
 VALUES (1, 'ERROR', 789) 
 ON CONFLICT (sensorid) 
 DO UPDATE SET state = EXCLUDED.state, measurement = EXCLUDED.measurement;
```

## Dataflow pipeline

create environment variables

```shell
source my_config/create_env_vars.sh
```


