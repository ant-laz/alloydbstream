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

### Launch the pipeline onto Dataflow

execute the bash script, which uses env vars, to launch the dataflow job
```shell
./scripts/01_launch_pipeline.sh
```

