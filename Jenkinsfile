pipeline {
    agent any
    environment {
        SONARQUBE_SERVER = 'SonarQube'
        SONAR_PROJECT_KEY = 'universal-jenkins-pipeline'
        SONAR_HOST = 'http://sonarqube:9000'
        CHECKOV_DIRECTORY = '.'
        DEPTRACK_URL = 'https://dependency-track.example.com'
        NOTIFY_EMAIL = 'devops@example.com'
    }
    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/example/repo.git', description: 'Git repository URL to clone')
        string(name: 'GIT_BRANCH', defaultValue: 'main', description: 'Git branch to build')
    }
    stages {
        stage('Checkout') {
            steps {
                script {
                    def gitClone = load 'src/gitClone.groovy'
                    gitClone(url: params.GIT_URL, branch: params.GIT_BRANCH)
                }
            }
        }
        stage('SAST - SonarQube') {
            steps {
                script {
                    def sonar = load 'src/sonarqubeScanner.groovy'
                    sonar(projectKey: env.SONAR_PROJECT_KEY, host: env.SONAR_HOST, login: env.SONAR_TOKEN)
                }
            }
        }
        stage('IaC - Checkov') {
            steps {
                script {
                    def checkov = load 'src/checkovScanner.groovy'
                    checkov(directory: env.CHECKOV_DIRECTORY)
                }
            }
        }
        stage('SCA - Dependency-Track') {
            steps {
                script {
                    def depTrack = load 'utils/dependencyTrack.groovy'
                    depTrack(url: env.DEPTRACK_URL, apiKey: env.DEPTRACK_API_KEY)
                }
            }
        }
    }
    post {
        success {
            script {
                def notifier = load 'utils/notify.groovy'
                notifier.notify(recipients: env.NOTIFY_EMAIL, status: 'SUCCESS')
            }
        }
        failure {
            script {
                def notifier = load 'utils/notify.groovy'
                notifier.notify(recipients: env.NOTIFY_EMAIL, status: 'FAILURE')
            }
        }
        unstable {
            script {
                def notifier = load 'utils/notify.groovy'
                notifier.notify(recipients: env.NOTIFY_EMAIL, status: 'UNSTABLE')
            }
        }
    }
}
