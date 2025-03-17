#  Copyright 2025 Google LLC
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

// Terraform local variables
// https://developer.hashicorp.com/terraform/language/values/locals
// to avoid repeating the same values or expr multiple times in this config
//
locals {
  repo_codename = "alloydbstream"
  max_dataflow_workers     = 1
  worker_disk_size_gb      = 200
  machine_type             = "g2-standard-4"
  dataflow_vm_public_ip    = false
}


// Google Cloud Project
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/project
// IF var.project_create = TRUE THEN project_reuse = null THEN new project made
// IF var.project_create = FALSE THEN project_reuse = {} THEN existing proj used
module "google_cloud_project" {
  source          = "github.com/GoogleCloudPlatform/cloud-foundation-fabric//modules/project?ref=v38.0.0"
  billing_account = var.billing_account
  project_reuse   = var.project_create ? null : {}
  name            = var.project_id
  parent          = var.organization
  services = [
    "serviceusage.googleapis.com",
    "servicenetworking.googleapis.com",  //for Private Service Access
    "dataflow.googleapis.com",
    "monitoring.googleapis.com",
    "alloydb.googleapis.com",
    "managedkafka.googleapis.com",
  ]
}

// Google Cloud Storage Bucket
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/gcs
module "buckets" {
  source        = "github.com/GoogleCloudPlatform/cloud-foundation-fabric//modules/gcs?ref=v38.0.0"
  project_id    = module.google_cloud_project.project_id
  name          = "${module.google_cloud_project.project_id}-${local.repo_codename}"
  location      = var.region
  storage_class = "STANDARD"
  force_destroy = var.destroy_all_resources
}

// Google Cloud Service Account
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/iam-service-account
module "dataflow_sa" {
  source     = "github.com/GoogleCloudPlatform/cloud-foundation-fabric//modules/iam-service-account?ref=v38.0.0"
  project_id = module.google_cloud_project.project_id
  name       = "${local.repo_codename}-sa"
  iam_project_roles = {
    (module.google_cloud_project.project_id) = [
      "roles/storage.admin",
      "roles/dataflow.worker",
      "roles/dataflow.admin",
      "roles/storage.objectAdmin",
      "roles/alloydb.client", //to write to AlloyDB
      "roles/serviceusage.serviceUsageConsumer" //to write to AlloyDB
    ]
  }
}

// Google Virtual Private Cloud (VPC) Network
// https://cloud.google.com/vpc/docs/vpc
// https://cloud.google.com/dataflow/docs/guides/specifying-networks
// https://cloud.google.com/vpc/docs/private-google-access
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/net-vpc
// [subnet.region]
// >> Subnets are regional resources.
// >> Subnetwork region must equal the zone where you run your Dataflow workers.
// [subnet.ip_cidr_range]
// >> #num of available IP addresses is a limit on the #num of Dataflow workers
// >> Google Cloud uses 1st 2 & last 2 IPv4 addr in subnet primary IPv4 addr
// >> IF ip_cidr_range = "10.0.0.0/24" THEN 252 ip addresses & #num workers
// >> IF ip_cidr_range = "10.1.0.0/16" THEN 65532 ip addresses & #num workers
// [enable_private_access]
// >> This is to do with Private Google Access
// >> VM instances that only have internal IP addresses (no external IP addr)
// >> can use Private Google Access tp reach IP addr of Google APIs and services
// [psa_configs]
// >> The Private Service Access configuration.
// >> https://cloud.google.com/vpc/docs/configure-private-services-access
// >> Service Producer = AlloyDB
// >> Steps to set up :
// >> 1. Allocate an IP address range (CIDR block) in your VPC network
// >>    This range will not be accessible to subnets
// >> 2. Create a private connection to a service producer (ALloyDB)
// >> the Terraform snippet "psa_config" takes care of #1 & #2 above
module "vpc_network" {
  source     = "github.com/GoogleCloudPlatform/cloud-foundation-fabric//modules/net-vpc?ref=v38.0.0"
  project_id = module.google_cloud_project.project_id
  name       = "${var.network_prefix}-net"
  psa_configs = [{
    ranges = { alloydb = "10.60.0.0/16" }
  }]
  subnets = [
    {
      ip_cidr_range         = "10.1.0.0/16"
      name                  = "${var.network_prefix}-subnet"
      region                = var.region
      enable_private_access = true
      secondary_ip_ranges = {
        pods     = "10.16.0.0/14"
        services = "10.20.0.0/24"
      }
    }
  ]
}

// Google Cloud VPC Firewall
// https://cloud.google.com/firewall/docs/about-firewalls
// https://cloud.google.com/dataflow/docs/guides/routes-firewall
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/net-vpc-firewall
// >> Ingress rule permits Dataflow VMs to receive packets from each other
// >> Egress rule permits Dataflow VMs to send packets to each other
// >> streaming : Dataflow VMs  send/receive network traffic on TCP port 12345
// >> batch : Dataflow VMs send/receive network traffic on TCP port 12346
module "firewall_rules" {
  // Default rules for internal traffic + SSH access via IAP
  source     = "github.com/GoogleCloudPlatform/cloud-foundation-fabric//modules/net-vpc-firewall?ref=v38.0.0"
  project_id = module.google_cloud_project.project_id
  network    = module.vpc_network.name
  default_rules_config = {
    admin_ranges = [
      module.vpc_network.subnet_ips["${var.region}/${var.network_prefix}-subnet"],
    ]
  }
  egress_rules = {
    allow-egress-dataflow = {
      deny        = false
      description = "Dataflow firewall rule egress"
      targets     = ["dataflow"]
      rules       = [{ protocol = "tcp", ports = [12345, 12346] }]
    }
  }
  ingress_rules = {
    allow-ingress-dataflow = {
      description = "Dataflow firewall rule ingress"
      targets     = ["dataflow"]
      rules       = [{ protocol = "tcp", ports = [12345, 12346] }]
    }
  }
}

// Google Cloud AlloyDB for Postgres
// https://github.com/GoogleCloudPlatform/cloud-foundation-fabric/tree/master/modules/alloydb
// https://cloud.google.com/alloydb/docs/about-private-services-access
module "alloydb" {
  source        = "github.com/GoogleCloudPlatform/cloud-foundation-fabric.git//modules/alloydb?ref=master"
  project_id    = module.google_cloud_project.project_id
  cluster_name  = "${local.repo_codename}-cluster"
  location      = var.region
  instance_name = "${local.repo_codename}-instance"
  network_config = {
    psa_config = {
      network = module.vpc_network.id
    }
  }
}

// Google Cloud Managed Service for Apache Kafka
// Kafka Cluster
resource "google_managed_kafka_cluster" "alloydbstream_kafka_cluster" {
  project    = module.google_cloud_project.project_id
  cluster_id = "${local.repo_codename}-kafka-cluster"
  location   = var.region
  capacity_config {
    vcpu_count   = 3
    memory_bytes = 3221225472
  }
  gcp_config {
    access_config {
      network_configs {
        subnet = module.vpc_network.subnet_ids["${var.region}/${var.network_prefix}-subnet"]
      }
    }
  }
}

// Google Cloud Managed Service for Apache Kafka
// Kafka Topic
# resource "google_managed_kafka_topic" "alloydbstream_kafka_topic" {
#   project            = data.google_project.default.project_id # Replace this with your project ID in quotes
#   topic_id           = "my-topic-id"
#   cluster            = google_managed_kafka_cluster.default.cluster_id
#   location           = "us-central1"
#   partition_count    = 2
#   replication_factor = 3
# }


// https://registry.terraform.io/providers/hashicorp/local/latest/docs/resources/file
// Generates a local file with the given content.
// Makes devops easier as bash scripts will use IDs of terraformed resources
// Example bash script create environmental variables will use IDs of
// Google Cloud resources created by Terraform.
resource "local_file" "variables_script" {
  filename        = "${path.module}/../scripts/00_set_variables.sh"
  file_permission = "0644"
  content         = <<FILE
# This file is generated by the Terraform code of this Solution Guide.
# We recommend that you modify this file only through the Terraform deployment.

export PROJECT=${module.google_cloud_project.project_id}
export REGION=${var.region}
export SUBNETWORK=regions/${var.region}/subnetworks/${var.network_prefix}-subnet
export TEMP_LOCATION=gs://${module.buckets.name}/tmp
export SERVICE_ACCOUNT=${module.dataflow_sa.email}
export DATAFLOW_VM_PUBLIC_IP=${local.dataflow_vm_public_ip}

export MAX_DATAFLOW_WORKERS=${local.max_dataflow_workers}
export DISK_SIZE_GB=${local.worker_disk_size_gb}
export MACHINE_TYPE=${local.machine_type}
FILE
}