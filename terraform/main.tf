locals {
  children_services = join(",", [
    for name, _child in var.children : "${name}=http://backend-${name}:3000"
  ])

  children_db_names = {
    for name, _child in var.children : name => "futurekawa_${name}"
  }
}

resource "docker_network" "futurekawa" {
  name = "futurekawa-network"
}

resource "docker_volume" "child_db_data" {
  for_each = var.children
  name     = "futurekawa-db-${each.key}-data"
}

resource "docker_container" "child_db" {
  for_each = var.children

  name  = "postgres-${each.key}"
  image = "postgres:16-alpine"
  env = [
    "POSTGRES_DB=${local.children_db_names[each.key]}",
    "POSTGRES_USER=${var.child_db_user}",
    "POSTGRES_PASSWORD=${var.child_db_password}"
  ]
  restart = "unless-stopped"

  upload {
    file   = "/docker-entrypoint-initdb.d/init.sql"
    source = "${path.module}/../services/backend-child/Bdd/MSPR5_BDD-1778060454.sql"
  }

  mounts {
    type   = "volume"
    source = docker_volume.child_db_data[each.key].name
    target = "/var/lib/postgresql/data"
  }

  networks_advanced {
    name = docker_network.futurekawa.name
  }
}

resource "docker_container" "mosquitto" {
  name    = "mosquitto-broker"
  image   = "eclipse-mosquitto:2"
  restart = "unless-stopped"

  upload {
    content = "listener 1883 0.0.0.0\nallow_anonymous true\n"
    file    = "/mosquitto/config/mosquitto.conf"
  }

  networks_advanced {
    name = docker_network.futurekawa.name
  }

  ports {
    internal = 1883
    external = 1883
  }
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
    "COUNTRY=${each.value.country}",
    "DB_HOST=postgres-${each.key}",
    "DB_PORT=5432",
    "DB_NAME=${local.children_db_names[each.key]}",
    "DB_USER=${var.child_db_user}",
    "DB_PASSWORD=${var.child_db_password}",
    "MQTT_BROKER=tcp://mosquitto-broker:1883",
    "ENTREPOT_ID=${each.value.entrepot_id}",
    "MQTT_INTERVAL_SECONDS=300"
  ]
  restart = "unless-stopped"

  networks_advanced {
    name = docker_network.futurekawa.name
  }

  ports {
    internal = 3000
    external = each.value.external_port
  }

  depends_on = [docker_container.child_db, docker_container.mosquitto]
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
