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
node {
  stage('Run Universal Pipeline') {
    def universal = load 'ci/universalPipeline.groovy'
    universal.call(
      GIT_URL: params?.GIT_URL ?: env.GIT_URL ?: '',
      GIT_BRANCH: params?.GIT_BRANCH ?: env.GIT_BRANCH ?: 'main',
      ENVIRONMENT: params?.ENVIRONMENT ?: 'development',
      REGISTRY_URL: params?.REGISTRY_URL ?: env.REGISTRY_URL
    )
  }
}
