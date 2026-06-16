variable "frontend_port" {
  description = "Port expose du frontend"
  type        = number
  default     = 8080
}

variable "mother_port" {
  description = "Port expose du backend mere"
  type        = number
  default     = 3200
}

variable "child_db_user" {
  description = "Utilisateur PostgreSQL pour les bases des backends filles"
  type        = string
  default     = "futurekawa"
}

variable "child_db_password" {
  description = "Mot de passe PostgreSQL pour les bases des backends filles"
  type        = string
  sensitive   = true
  default     = "futurekawa_pwd"
}

variable "children" {
  description = "Liste des backends filles a deployer"
  type = map(object({
    country       = string
    external_port = number
  }))

  default = {
    brazil = {
      country       = "Brazil"
      external_port = 3101
    }
    ecuador = {
      country       = "Ecuador"
      external_port = 3102
    }
    colombia = {
      country       = "Colombia"
      external_port = 3103
    }
  }
}
