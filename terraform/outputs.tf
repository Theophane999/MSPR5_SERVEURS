output "frontend_url" {
  value = "http://localhost:${var.frontend_port}"
}

output "backend_mother_url" {
  value = "http://localhost:${var.mother_port}/api/children"
}

output "backend_children_urls" {
  value = {
    for name, child in var.children : name => "http://localhost:${child.external_port}/api/info"
  }
}
