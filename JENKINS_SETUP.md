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
