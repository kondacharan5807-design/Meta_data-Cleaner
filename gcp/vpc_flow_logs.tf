# Google Cloud Platform (GCP) VPC Network & VPC Flow Logs Blueprint
# Deep Network Traffic Inspection & Audit Compliance
# MetaShield Enclave Architecture

resource "google_compute_network" "metashield_vpc" {
  name                    = "metashield-isolated-vpc"
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "metashield_subnet" {
  name          = "metashield-enclave-subnet"
  ip_cidr_range = "10.100.0.0/24"
  region        = var.region
  network       = google_compute_network.metashield_vpc.id

  # MANDATORY COMPLIANCE: Deep Network Flow Logging
  log_config {
    aggregation_interval = "INTERVAL_5_SEC"
    flow_sampling        = 1.0  # 100% sample rate for non-repudiation audit
    metadata             = "INCLUDE_ALL_METADATA"
    metadata_fields      = []
  }

  private_ip_google_access = true
}

# Firewall Rule: Internal Only
resource "google_compute_firewall" "allow_internal" {
  name    = "metashield-allow-internal"
  network = google_compute_network.metashield_vpc.name

  allow {
    protocol = "tcp"
    ports    = ["8080", "443"]
  }

  source_ranges = ["10.100.0.0/24"]
  target_tags   = ["metashield-enclave"]
}
