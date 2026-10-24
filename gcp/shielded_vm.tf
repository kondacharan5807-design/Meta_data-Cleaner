# Google Cloud Platform (GCP) Shielded VM Blueprint
# MetaShield Enclave Secure Compute Deployment
# Architects: Charan Konda (2511CS040113) & J. Naga Phani (2511CS040132)
# Institution: Malla Reddy University - CS BETA - 17

terraform {
  required_version = ">= 1.5.0"
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
}

variable "project_id" {
  description = "GCP Project ID"
  type        = string
  default     = "metashield-enclave-prod"
}

variable "region" {
  description = "GCP Compute Region"
  type        = string
  default     = "asia-south1" # Mumbai / Hyderabad vicinity
}

variable "zone" {
  description = "GCP Compute Zone"
  type        = string
  default     = "asia-south1-a"
}

provider "google" {
  project = var.project_id
  region  = var.region
}

# Service Account with Minimal Privilege
resource "google_service_account" "metashield_sa" {
  account_id   = "metashield-node-sa"
  display_name = "MetaShield Production Node Service Account"
}

# Shielded VM Compute Instance
resource "google_compute_instance" "metashield_shielded_vm" {
  name         = "metashield-enclave-node-01"
  machine_type = "e2-standard-4"
  zone         = var.zone

  tags = ["metashield-enclave", "isolated-compute"]

  boot_disk {
    initialize_params {
      image = "ubuntu-os-cloud/ubuntu-2204-lts"
      size  = 50
      type  = "pd-ssd"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.metashield_subnet.id
    # Private only: zero public IP assignment for confidential enclave isolation
  }

  # HARDENED SHIELDED VM SPECIFICATION (MANDATORY COMPLIANCE)
  shielded_instance_config {
    enable_secure_boot          = true  # Protects against rootkits/bootkits
    enable_vtpm                 = true  # Virtual Trusted Platform Module for measured boot
    enable_integrity_monitoring = true  # Automated baseline integrity validation
  }

  service_account {
    email  = google_service_account.metashield_sa.email
    scopes = ["cloud-platform"]
  }

  metadata = {
    block-project-ssh-keys = "true"
    enable-oslogin         = "TRUE"
  }

  metadata_startup_script = <<-EOF
    #!/bin/bash
    set -e
    apt-get update && apt-get install -y docker.io
    systemctl start docker
    systemctl enable docker
    echo "MetaShield Shielded VM Enclave Initialized."
  EOF
}
