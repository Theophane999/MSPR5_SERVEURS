# GCP Production Infra (No Pre-prod)

This Terraform stack creates production-only infrastructure on GCP:
- 1 VM for Jenkins CI/CD
- 1 VM for mother services (backend + frontend)
- N VMs for country nodes (backend-child + PostgreSQL + Mosquitto)

## What gets created
- 1 custom VPC + subnet
- Firewall rules for SSH, Jenkins UI, mother web/API, country MQTT/API
- Reserved static public IPs for Jenkins, mother, and each country VM
- Jenkins VM with Docker + Jenkins container
- Mother VM with Docker Compose stack
- Country VMs with Docker Compose stacks
- Optional Cloud DNS zone + A records (`app`, `api`, `jenkins`, and per-country)

## Prerequisites
- Terraform >= 1.6
- gcloud CLI authenticated (`gcloud auth application-default login`)
- Existing Docker images pushed as:
  - `futurekawa/backend-child:prod`
  - `futurekawa/backend-mother:prod`
  - `futurekawa/frontend:prod`

## Quick start
1. Copy vars file:
   - `cp terraform.tfvars.example terraform.tfvars`
2. Fill required values in `terraform.tfvars`:
   - `gcp_project_id`
   - `ssh_public_key`
   - `child_db_password`
3. Deploy:
   - `terraform init`
   - `terraform plan`
   - `terraform apply`

## Optional DNS enablement
If you own a domain, set these values in `terraform.tfvars`:
- `dns_domain_name = "your-domain.tld"`
- `dns_managed_zone_name = "futurekawa-prod-zone"`

After `terraform apply`, copy `dns_name_servers` output into your registrar DNS settings.

Default records created when DNS is enabled:
- `app.<domain>` -> mother VM public IP
- `api.<domain>` -> mother VM public IP
- `jenkins.<domain>` -> Jenkins VM public IP
- `<country>.<domain>` -> each country VM public IP (can be disabled with `dns_create_country_records = false`)

## Important production notes
- Restrict `admin_ingress_cidrs` and `iot_ingress_cidrs` before go-live.
- Move DB passwords and registry credentials to Secret Manager in a next iteration.
- Put TLS/HTTPS in front of mother frontend (LB or reverse proxy).
- Jenkins should be behind IP allow-list and ideally SSO.

## Scale country nodes
Edit `country_servers` map in `terraform.tfvars` to add/remove countries.
