variable "gcp_project_id" {
  description = "GCP project ID"
  type        = string
}

variable "gcp_region" {
  description = "Primary GCP region"
  type        = string
  default     = "europe-west1"
}

variable "network_name" {
  description = "Custom VPC name"
  type        = string
  default     = "futurekawa-prod-vpc"
}

variable "subnet_cidr" {
  description = "CIDR block for the production subnet"
  type        = string
  default     = "10.50.0.0/16"
}

variable "name_prefix" {
  description = "Prefix used for compute instance names"
  type        = string
  default     = "futurekawa-prod"
}

variable "admin_ingress_cidrs" {
  description = "CIDR blocks allowed to access SSH and Jenkins UI"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "iot_ingress_cidrs" {
  description = "CIDR blocks allowed to publish to country MQTT brokers"
  type        = list(string)
  default     = ["0.0.0.0/0"]
}

variable "ssh_username" {
  description = "Linux username injected in instance metadata"
  type        = string
  default     = "ubuntu"
}

variable "ssh_public_key" {
  description = "Public SSH key content (without username prefix)"
  type        = string
}

variable "jenkins_zone" {
  description = "Zone for Jenkins instance"
  type        = string
  default     = "europe-west1-b"
}

variable "mother_zone" {
  description = "Zone for mother platform instance"
  type        = string
  default     = "europe-west1-b"
}

variable "jenkins_machine_type" {
  description = "Machine type for Jenkins VM"
  type        = string
  default     = "e2-standard-2"
}

variable "mother_machine_type" {
  description = "Machine type for mother VM"
  type        = string
  default     = "e2-standard-2"
}

variable "country_machine_type_default" {
  description = "Default machine type for country VMs"
  type        = string
  default     = "e2-medium"
}

variable "country_servers" {
  description = "Country servers to provision"
  type = map(object({
    zone          = string
    entrepot_id   = number
    country_label = string
    machine_type  = optional(string)
  }))

  default = {
    brazil = {
      zone          = "europe-west1-c"
      entrepot_id   = 1
      country_label = "Brazil"
    }
    ecuador = {
      zone          = "europe-west1-c"
      entrepot_id   = 2
      country_label = "Ecuador"
    }
    colombia = {
      zone          = "europe-west1-c"
      entrepot_id   = 3
      country_label = "Colombia"
    }
  }
}

variable "child_db_password" {
  description = "PostgreSQL password used in each country node"
  type        = string
  sensitive   = true
}

variable "db_init_sql_url" {
  description = "URL to bootstrap SQL for child databases"
  type        = string
  default     = "https://raw.githubusercontent.com/Theophane999/MSPR5_SERVEURS/main/services/backend-child/Bdd/MSPR5_BDD-1778060454.sql"
}

variable "docker_registry" {
  description = "Registry host used by docker login in Jenkins (ex: ghcr.io or index.docker.io)"
  type        = string
  default     = "index.docker.io"
}

variable "backend_mother_public_url" {
  description = "Public URL consumed by frontend container"
  type        = string
  default     = ""
}

variable "dns_domain_name" {
  description = "Optional DNS domain for production (example: future-kawa.com). Leave empty to disable Cloud DNS resources."
  type        = string
  default     = ""
}

variable "dns_managed_zone_name" {
  description = "Cloud DNS managed zone name (used only when dns_domain_name is set)"
  type        = string
  default     = "futurekawa-prod-zone"
}

variable "dns_create_country_records" {
  description = "Create country subdomains (<country>.<domain>) when DNS is enabled"
  type        = bool
  default     = true
}

variable "service_account_email" {
  description = "Optional service account email for VMs. Leave empty to use default compute service account."
  type        = string
  default     = ""
}