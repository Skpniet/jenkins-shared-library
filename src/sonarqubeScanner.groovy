def call(Map config = [:]) {
    def projectKey = config.projectKey ?: 'universal-jenkins-pipeline'
    def hostUrl = config.host ?: 'http://sonarqube:9000'
    def loginToken = config.login ?: env.SONAR_TOKEN

    if (!loginToken) {
        error 'SONAR_TOKEN is not set. Configure credentials in Jenkins environment or pipeline.'
    }

    sh '''#!/bin/bash
        sonar-scanner \
            -Dsonar.projectKey=${projectKey} \
            -Dsonar.sources=. \
            -Dsonar.host.url=${hostUrl} \
            -Dsonar.login=${loginToken}
    '''
}
return this
