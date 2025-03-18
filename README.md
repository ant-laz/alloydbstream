## What problem is being addressed here?

Streaming data from Kafka to AlloyDB for PostgreSQL using Dataflow.

This is a hard problem for the following reasons:
 * There are no Google published templates to do this in Java or Python.
 * Apache Beam JdbcIO lacks documentation around constructing URLs for AlloyDB
 * Connectivity bewteen Dataflow workers & AlloyDB is non-trivial to setup
 * There are many configuration settings to tweak to achieve a high throughput
 * UPSERTS into AlloyDB, with a high throughput, is not straightforward.

## How to execute this solution.

### Set up an environment on Google CLoud using Terraform

Life is short, so let's use Terraform to first set up an env on Google Cloud.

Change the working directory to the terraform folder
```shell
cd terraform
```

Create a file named terraform.tfvars 
```shell
touch terraform.tfvars
```

Add the following configuration variables to the file, with your own values.
```shell
billing_account = "YOUR_BILLING_ACCOUNT"
organization = "YOUR_ORGANIZATION_ID"
project_create = true/false
project_id = "YOUR_PROJECT_ID"
region = "YOUR_REGION"
database = "NAME_OF_YOUR_ALLOYDB_DATABASE"
user = "USER_ON_YOUR_ALLOYDB_DATABASE"
password = "USER_PASSWORD_ON_YOUR_ALLOYDB_DATABASE"
```

Run the following command to initialize Terraform:
```shell
terraform init
```

Run the following command to apply the Terraform configuration.
```shell
terraform apply
```

navigate up to the root of the repository
```shell
cd ..
```

execute the bash script, created by terraform, to make some env vars
```shell
source scripts/00_set_variables.sh
```

### In the Terraform created AlloyDB Instance create a Database

Use AlloyDB studio to login to the default `postgres` database in the cluster

From there execute the following to create a database
```shell
CREATE DATABASE automation;
```

In AlloyDB switch to the newly created `automation` database.

And a table
```shell
CREATE TABLE sensors (measurement INTEGER,
                      state VARCHAR(255),
                      sensorID SERIAL PRIMARY KEY);
```

And initialize the table with some dummy rows
```shell
INSERT INTO sensors (state, measurement) values ('ACTIVE', 123);
INSERT INTO sensors (state, measurement) values ('ACTIVE', 456);
```

And another table
```shell
CREATE TABLE messages (message VARCHAR(255),
                      messageID SERIAL PRIMARY KEY);
```

And initialize the table with some dummy rows
```shell
INSERT INTO messages (message) values ('TEST_MSG_1');
INSERT INTO messages (message) values ('TEST_MSG_2');
```

### Launch the pipeline onto Dataflow

execute the bash script, which uses env vars, to launch the dataflow job
```shell
./scripts/01_launch_pipeline.sh
```

