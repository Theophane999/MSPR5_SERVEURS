locals {
  children_services = join(",", [
    for name, _child in var.children : "${name}=http://backend-${name}:3000"
  ])
}

resource "docker_network" "futurekawa" {
  name = "futurekawa-network"
}

resource "docker_image" "backend_child" {
  name = "futurekawa/backend-child:latest"

  build {
    context = "${path.module}/../services/backend-child"
  }
}

resource "docker_container" "backend_child" {
  for_each = var.children

  name  = "backend-${each.key}"
  image = docker_image.backend_child.image_id
  env = [
    "PORT=3000",
    "COUNTRY=${each.value.country}"
  ]
  restart = "unless-stopped"

  networks_advanced {
    name = docker_network.futurekawa.name
  }

  ports {
    internal = 3000
    external = each.value.external_port
  }
}

resource "docker_image" "backend_mother" {
  name = "futurekawa/backend-mother:latest"

  build {
    context = "${path.module}/../services/backend-mother"
  }

  depends_on = [docker_image.backend_child]
}

resource "docker_container" "backend_mother" {
  name  = "backend-mother"
  image = docker_image.backend_mother.image_id
  env = [
    "PORT=3000",
    "CHILDREN_SERVICES=${local.children_services}"
  ]
  restart = "unless-stopped"

  networks_advanced {
    name = docker_network.futurekawa.name
  }

  ports {
    internal = 3000
    external = var.mother_port
  }

  depends_on = [docker_container.backend_child]
}

resource "docker_image" "frontend" {
  name = "futurekawa/frontend:latest"

  build {
    context = "${path.module}/../services/frontend"
  }

  depends_on = [docker_image.backend_mother]
}

resource "docker_container" "frontend" {
  name  = "frontend-futurekawa"
  image = docker_image.frontend.image_id
  env = [
    "BACKEND_MOTHER_PUBLIC_URL=http://localhost:${var.mother_port}"
  ]
  restart = "unless-stopped"

  networks_advanced {
    name = docker_network.futurekawa.name
  }

  ports {
    internal = 80
    external = var.frontend_port
  }

  depends_on = [docker_container.backend_mother]
}
