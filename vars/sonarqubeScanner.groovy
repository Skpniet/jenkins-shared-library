def call(Map config = [:]) {
    def projectKey = config.projectKey ?: "${env.JOB_NAME}-${env.BUILD_NUMBER}"
    def host = config.host ?: env.SONAR_HOST
    def tokenCred = config.tokenCred ?: 'sonar-token'

    withCredentials([string(credentialsId: tokenCred, variable: 'SONAR_TOKEN')]) {
        sh "sonar-scanner -Dsonar.projectKey=${projectKey} -Dsonar.sources=. -Dsonar.host.url=${host} -Dsonar.login=${SONAR_TOKEN} || true"
    }
}
return this
