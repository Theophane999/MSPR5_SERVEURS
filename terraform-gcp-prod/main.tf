locals {
  ssh_metadata_value = "${var.ssh_username}:${var.ssh_public_key}"

  dns_enabled     = trimspace(var.dns_domain_name) != ""
  dns_domain_fqdn = local.dns_enabled ? "${trim(var.dns_domain_name, ".")}." : ""

  country_services = [
    for name, ip in google_compute_address.country :
    "${name}=http://${ip.address}:3000"
  ]

  children_services_string = join(",", local.country_services)

  default_service_account = var.service_account_email == "" ? null : var.service_account_email
}

resource "google_compute_address" "jenkins" {
  name   = "${var.name_prefix}-jenkins-ip"
  region = var.gcp_region
}

resource "google_compute_address" "mother" {
  name   = "${var.name_prefix}-mother-ip"
  region = var.gcp_region
}

resource "google_compute_address" "country" {
  for_each = var.country_servers

  name   = "${var.name_prefix}-country-${each.key}-ip"
  region = var.gcp_region
}

resource "google_compute_network" "prod" {
  name                    = var.network_name
  auto_create_subnetworks = false
}

resource "google_compute_subnetwork" "prod" {
  name          = "${var.network_name}-subnet"
  ip_cidr_range = var.subnet_cidr
  region        = var.gcp_region
  network       = google_compute_network.prod.id
}

resource "google_compute_firewall" "ssh" {
  name    = "${var.name_prefix}-allow-ssh"
  network = google_compute_network.prod.name

  allow {
    protocol = "tcp"
    ports    = ["22"]
  }

  source_ranges = var.admin_ingress_cidrs
  target_tags   = ["futurekawa-ssh"]
}

resource "google_compute_firewall" "jenkins_ui" {
  name    = "${var.name_prefix}-allow-jenkins-ui"
  network = google_compute_network.prod.name

  allow {
    protocol = "tcp"
    ports    = ["8080"]
  }

  source_ranges = var.admin_ingress_cidrs
  target_tags   = ["futurekawa-jenkins"]
}

resource "google_compute_firewall" "mother_web" {
  name    = "${var.name_prefix}-allow-mother-web"
  network = google_compute_network.prod.name

  allow {
    protocol = "tcp"
    ports    = ["80", "443", "3200"]
  }

  source_ranges = ["0.0.0.0/0"]
  target_tags   = ["futurekawa-mother"]
}

resource "google_compute_firewall" "country_mqtt" {
  name    = "${var.name_prefix}-allow-country-mqtt"
  network = google_compute_network.prod.name

  allow {
    protocol = "tcp"
    ports    = ["1883"]
  }

  source_ranges = var.iot_ingress_cidrs
  target_tags   = ["futurekawa-country"]
}

resource "google_compute_firewall" "country_api" {
  name    = "${var.name_prefix}-allow-country-api"
  network = google_compute_network.prod.name

  allow {
    protocol = "tcp"
    ports    = ["3000"]
  }

  source_ranges = var.admin_ingress_cidrs
  target_tags   = ["futurekawa-country"]
}

resource "google_compute_instance" "jenkins" {
  name         = "${var.name_prefix}-jenkins"
  zone         = var.jenkins_zone
  machine_type = var.jenkins_machine_type
  tags         = ["futurekawa-ssh", "futurekawa-jenkins"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 30
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.prod.id
    access_config {
      nat_ip = google_compute_address.jenkins.address
    }
  }

  metadata = {
    ssh-keys = local.ssh_metadata_value
  }

  metadata_startup_script = templatefile("${path.module}/templates/startup_jenkins.sh.tftpl", {
    docker_registry = var.docker_registry
  })

  service_account {
    email  = local.default_service_account
    scopes = ["cloud-platform"]
  }
}

resource "google_compute_instance" "country" {
  for_each = var.country_servers

  name         = "${var.name_prefix}-country-${each.key}"
  zone         = each.value.zone
  machine_type = coalesce(try(each.value.machine_type, null), var.country_machine_type_default)
  tags         = ["futurekawa-ssh", "futurekawa-country"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 30
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.prod.id
    access_config {
      nat_ip = google_compute_address.country[each.key].address
    }
  }

  metadata = {
    ssh-keys = local.ssh_metadata_value
  }

  metadata_startup_script = templatefile("${path.module}/templates/startup_country.sh.tftpl", {
    country_name    = each.key
    country_label   = each.value.country_label
    entrepot_id     = each.value.entrepot_id
    db_password     = var.child_db_password
    db_init_sql_url = var.db_init_sql_url
  })

  service_account {
    email  = local.default_service_account
    scopes = ["cloud-platform"]
  }
}

resource "google_compute_instance" "mother" {
  name         = "${var.name_prefix}-mother"
  zone         = var.mother_zone
  machine_type = var.mother_machine_type
  tags         = ["futurekawa-ssh", "futurekawa-mother"]

  boot_disk {
    initialize_params {
      image = "debian-cloud/debian-12"
      size  = 30
      type  = "pd-balanced"
    }
  }

  network_interface {
    subnetwork = google_compute_subnetwork.prod.id
    access_config {
      nat_ip = google_compute_address.mother.address
    }
  }

  metadata = {
    ssh-keys = local.ssh_metadata_value
  }

  metadata_startup_script = templatefile("${path.module}/templates/startup_mother.sh.tftpl", {
    children_services      = local.children_services_string
    backend_mother_pub_url = var.backend_mother_public_url
  })

  service_account {
    email  = local.default_service_account
    scopes = ["cloud-platform"]
  }

  lifecycle {
    ignore_changes = [metadata_startup_script]
  }

  depends_on = [google_compute_instance.country]
}

resource "google_dns_managed_zone" "prod" {
  count = local.dns_enabled ? 1 : 0

  name        = var.dns_managed_zone_name
  dns_name    = local.dns_domain_fqdn
  description = "FutureKawa production DNS zone"
}

resource "google_dns_record_set" "app" {
  count = local.dns_enabled ? 1 : 0

  name         = "app.${local.dns_domain_fqdn}"
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.prod[0].name
  rrdatas      = [google_compute_address.mother.address]
}

resource "google_dns_record_set" "api" {
  count = local.dns_enabled ? 1 : 0

  name         = "api.${local.dns_domain_fqdn}"
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.prod[0].name
  rrdatas      = [google_compute_address.mother.address]
}

resource "google_dns_record_set" "jenkins" {
  count = local.dns_enabled ? 1 : 0

  name         = "jenkins.${local.dns_domain_fqdn}"
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.prod[0].name
  rrdatas      = [google_compute_address.jenkins.address]
}

resource "google_dns_record_set" "country" {
  for_each = local.dns_enabled && var.dns_create_country_records ? google_compute_address.country : {}

  name         = "${each.key}.${local.dns_domain_fqdn}"
  type         = "A"
  ttl          = 300
  managed_zone = google_dns_managed_zone.prod[0].name
  rrdatas      = [each.value.address]
}