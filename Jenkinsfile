pipeline {
  agent any

  options {
    timestamps()
    disableConcurrentBuilds()
  }

  triggers {
    githubPush()
  }

  stages {
    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Backend Tests') {
      parallel {
        stage('backend-child') {
          steps {
            dir('services/backend-child') {
              bat 'gradle --no-daemon test'
            }
          }
        }

        stage('backend-mother') {
          steps {
            dir('services/backend-mother') {
              bat 'gradle --no-daemon test'
            }
          }
        }
      }
    }

    stage('Frontend Unit Tests') {
      steps {
        dir('services/frontend') {
          bat 'set "CHROME_BIN=C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe" && npm ci && npx ng test --watch=false --browsers=ChromeHeadlessNoSandbox --progress=false'
        }
      }
    }

    stage('Frontend Build') {
      steps {
        dir('services/frontend') {
          bat 'npm ci'
          bat 'npm run build'
        }
      }
    }

    stage('Terraform Validate') {
      steps {
        dir('terraform') {
          bat 'terraform init -backend=false'
          bat 'terraform fmt -check'
          bat 'terraform validate'
        }
      }
    }

    stage('Docker Build') {
      parallel {
        stage('backend-child image') {
          steps {
            bat 'docker build -t futurekawa/backend-child:ci .\\services\\backend-child'
          }
        }

        stage('backend-mother image') {
          steps {
            bat 'docker build -t futurekawa/backend-mother:ci .\\services\\backend-mother'
          }
        }

        stage('frontend image') {
          steps {
            bat 'docker build -t futurekawa/frontend:ci .\\services\\frontend'
          }
        }
      }
    }
  }

  post {
    always {
      junit allowEmptyResults: true, testResults: '**/build/test-results/test/*.xml'
      archiveArtifacts allowEmptyArchive: true, artifacts: 'services/frontend/dist/**'
    }
  }
}