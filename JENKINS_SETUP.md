# Jenkins Setup

Ce guide te permet de lancer la CI du projet avec Jenkins.

## 1) Plugins Jenkins requis

Installe au minimum:

- Pipeline
- Git
- GitHub Integration
- Credentials Binding
- JUnit
- Pipeline: Stage View

Pour la variante Docker ([Jenkinsfile.docker](Jenkinsfile.docker)) ajoute aussi:

- Docker Pipeline

## 2) Choix du pipeline

- Agent Windows avec outils installes localement: [Jenkinsfile](Jenkinsfile)
- Agent Linux/Unix avec outils encapsules par stage: [Jenkinsfile.docker](Jenkinsfile.docker)

## 3) Credentials GitHub

Dans Jenkins:

1. Dashboard -> Manage Jenkins -> Credentials -> Global -> Add Credentials
2. Type: Username with password ou Personal Access Token
3. ID conseille: github-credentials

## 3.bis) Credentials requis pour Jenkinsfile.docker (IDs exacts)

Si tu utilises [Jenkinsfile.docker](Jenkinsfile.docker), cree ces credentials avec les IDs exacts:

- `github-credentials` (Username with password ou Secret text PAT) pour l'acces au repo GitHub
- `ssh-prod-server` (SSH Username with private key) utilisateur + cle privee de la VM/serveur prod

Mode de deploiement actuel: pas de push vers un registry Docker.
Le deploy `main` fait un `git pull` puis un `docker compose up -d --build` via SSH sur plusieurs VMs en une seule execution.

Premier deploiement (VM vide): le pipeline bootstrap automatiquement la VM cible.
S'il ne trouve pas `/opt/futurekawa/.git`, il cree `/opt/futurekawa`, clone le repo, puis lance le deploiement.

Les VMs cibles sont configurees dans les parametres du job Jenkins (Build with Parameters):

- `PROD_HOST_MOTHER`
- `PROD_HOST_BRAZIL`
- `PROD_HOST_COLOMBIA`
- `PROD_HOST_ECUADOR`
- `PROD_HOST_JENKINS` (optionnel)

Renseigne au moins un `PROD_HOST_*`.

Prerequis minimum sur chaque VM cible:

- Docker + Docker Compose installes
- acces reseau sortant vers GitHub
- utilisateur SSH autorise a executer Docker (et sudo pour creer `/opt/futurekawa` au premier run)

Note: ce pipeline ne fait pas de provisionnement GCP par Terraform. Il deploie via SSH sur un hote deja provisionne.

## 4) Job recommande: Multibranch Pipeline

1. New Item -> MSPR5_SERVEURS-CI -> Multibranch Pipeline
2. Branch Sources -> Add source -> GitHub
3. Owner: Theophane999
4. Repository: MSPR5_SERVEURS
5. Credentials: github-credentials
6. Build Configuration -> by Jenkinsfile
7. Script Path:
- Jenkinsfile (Windows)
- Jenkinsfile.docker (Docker agents)
8. Save puis Scan Repository Now

## 5) Alternative: Pipeline simple

1. New Item -> MSPR5_SERVEURS-CI -> Pipeline
2. Pipeline definition: Pipeline script from SCM
3. SCM: Git
4. Repository URL: <PRIVATE_URL>
5. Credentials: github-credentials
6. Branches to build: */main
7. Script Path:
- Jenkinsfile (Windows)
- Jenkinsfile.docker (Docker agents)
8. Save puis Build Now

## 6) Webhook GitHub (build automatique)

Dans GitHub -> Settings -> Webhooks -> Add webhook:

- Payload URL: http://<jenkins-host>/github-webhook/
- Content type: application/json
- Events: Just the push event

Dans le job Jenkins, active le trigger GitHub webhook (les Jenkinsfiles incluent deja `githubPush()`).

## 7) Pre-check agent

### Si [Jenkinsfile](Jenkinsfile)

L'agent doit avoir:

- Java 21
- Gradle
- Node.js 20 + npm
- Terraform 1.6+
- Docker

### Si [Jenkinsfile.docker](Jenkinsfile.docker)

L'agent doit avoir:

- Docker (acces daemon)
- Droit d'executer des conteneurs

## 8) Validation rapide

Apres un build Jenkins, verifier que les stages passent:

- Backend Tests
- Frontend Build
- Terraform Validate
- Docker Build

Le stage `post` publie:

- rapports JUnit: `**/build/test-results/test/*.xml`
- artefacts frontend: `services/frontend/dist/**`

## 9) Comportement CI/CD recommande (prod + pre-prod)

Avec [Jenkinsfile.docker](Jenkinsfile.docker):

- branches `feature/*`: CI qualite uniquement (tests/build/validate)
- branche `pre-prod`: CI qualite + smoke local via `scripts/smoke-preprod.ps1` (sans deploiement distant)
- branche `main`: CI qualite + approbation manuelle + deploy prod + smoke post-deploy

## 10) Parametres URLs smoke prod

Le pipeline expose des parametres modifiables sans edition du Jenkinsfile:

- `PROD_FRONTEND_URL`
- `PROD_MOTHER_API_URL`
- `PROD_CHILD_HEALTH_BRAZIL`
- `PROD_CHILD_HEALTH_COLOMBIA`
- `PROD_CHILD_HEALTH_ECUADOR`

Il expose aussi les hotes de deploiement multi-VM:

- `PROD_HOST_MOTHER`
- `PROD_HOST_BRAZIL`
- `PROD_HOST_COLOMBIA`
- `PROD_HOST_ECUADOR`
- `PROD_HOST_JENKINS` (optionnel)

Par defaut ils pointent vers le domaine `future-kawa.online`.
Si la propagation DNS n'est pas terminee, remplace temporairement par les IP publiques GCP au lancement du build.
