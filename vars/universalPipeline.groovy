def call(Map params = [:]) {
    // Parameters and defaults
    def repoUrl = params.GIT_URL ?: env.GIT_URL ?: ''
    def branch = params.GIT_BRANCH ?: 'main'
    def envName = params.ENVIRONMENT ?: 'development'

    def sonarImage = params.SONAR_IMAGE ?: 'sonarsource/sonar-scanner-cli:4.7.0'
    def checkovImage = params.CHECKOV_IMAGE ?: 'bridgecrew/checkov:2.0.607'
    def syftImage = params.SYFT_IMAGE ?: 'anchore/syft:1.12.0'
    def curlImage = params.CURL_IMAGE ?: 'curlimages/curl:7.88.1'

    def podYaml = libraryResource('k8s/podTemplate.yaml') ?: null

    podTemplate(label: "universal-pipeline-${env.BUILD_ID}", yaml: podYaml, inheritFrom: '', containers: [
        containerTemplate(name: 'sonar', image: sonarImage, ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'checkov', image: checkovImage, ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'sbom', image: syftImage, ttyEnabled: true, command: 'cat'),
        containerTemplate(name: 'curl', image: curlImage, ttyEnabled: true, command: 'cat')
    ]) {
        node("universal-pipeline-${env.BUILD_ID}") {
            try {
                stage('Checkout') {
                    if (repoUrl) {
                        checkout([$class: 'GitSCM', branches: [[name: branch]], userRemoteConfigs: [[url: repoUrl]]])
                    } else {
                        checkout scm
                    }
                }

                stage('SAST - SonarQube') {
                    container('sonar') {
                        withCredentials([string(credentialsId: 'sonar-token', variable: 'SONAR_TOKEN')]) {
                            sh "sonar-scanner -Dsonar.projectKey=${env.JOB_NAME}-${env.BUILD_NUMBER} -Dsonar.sources=. -Dsonar.host.url=${env.SONAR_HOST} -Dsonar.login=${SONAR_TOKEN} || true"
                        }
                    }
                }

                stage('IaC - Checkov') {
                    container('checkov') {
                        sh 'checkov -d . -o compact || true'
                    }
                }

                stage('SCA - SBOM -> Dependency-Track') {
                    container('sbom') {
                        sh 'syft . -o cyclonedx-json=sbom.json'
                    }
                    container('curl') {
                        withCredentials([string(credentialsId: 'deptrack-api-key', variable: 'DEPT_API_KEY')]) {
                            sh "curl -X PUT \"${env.DEPTRACK_URL}/api/v1/bom\" -H \"X-Api-Key: ${DEPT_API_KEY}\" -H 'Content-Type: application/json' --data-binary @sbom.json || true"
                        }
                    }
                }

                stage('Archive') {
                    archiveArtifacts artifacts: 'sbom.json', allowEmptyArchive: true
                }

            } catch (err) {
                currentBuild.result = 'FAILURE'
                throw err
            } finally {
                // Notifications via utils/notify.groovy if present in library
                try {
                    def notifier = load 'utils/notify.groovy'
                    notifier.notify(recipients: env.NOTIFY_EMAIL ?: 'devops@example.com', status: currentBuild.currentResult)
                } catch (e) {
                    echo "notify helper not found or failed: ${e}"
                }
            }
        }
    }
}
return this
