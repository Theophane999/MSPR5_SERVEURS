output "jenkins_public_ip" {
  description = "Public IP of Jenkins VM"
  value       = google_compute_address.jenkins.address
}

output "mother_public_ip" {
  description = "Public IP of mother VM"
  value       = google_compute_address.mother.address
}

output "country_public_ips" {
  description = "Public IP by country node"
  value = {
    for k, ip in google_compute_address.country :
    k => ip.address
  }
}

output "children_services" {
  description = "Value to set for CHILDREN_SERVICES on backend mother"
  value       = local.children_services_string
}

output "jenkins_url" {
  value = local.dns_enabled ? "http://jenkins.${trim(var.dns_domain_name, ".")}" : "http://${google_compute_address.jenkins.address}:8080"
}

output "frontend_url" {
  value = local.dns_enabled ? "http://app.${trim(var.dns_domain_name, ".")}" : "http://${google_compute_address.mother.address}"
}

output "mother_api_url" {
  value = local.dns_enabled ? "http://api.${trim(var.dns_domain_name, ".")}:3200/api/children" : "http://${google_compute_address.mother.address}:3200/api/children"
}

output "dns_name_servers" {
  description = "Authoritative nameservers to configure at your registrar when DNS is enabled"
  value       = local.dns_enabled ? google_dns_managed_zone.prod[0].name_servers : []
}