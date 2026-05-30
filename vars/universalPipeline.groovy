def call(Map params = [:]) {
    def repoUrl = params.GIT_URL ?: env.GIT_URL ?: ''
    def branch = params.GIT_BRANCH ?: env.BRANCH_NAME ?: 'main'
    def envName = params.ENVIRONMENT ?: 'development'

    def sonarImage = params.SONAR_IMAGE ?: 'sonarsource/sonar-scanner-cli:4.7.0'
    def checkovImage = params.CHECKOV_IMAGE ?: 'bridgecrew/checkov:2.0.607'
    def syftImage = params.SYFT_IMAGE ?: 'anchore/syft:1.12.0'
    def trivyImage = params.TRIVY_IMAGE ?: 'aquasec/trivy:0.45.0'
    def dockerImage = params.DOCKER_IMAGE ?: 'docker:24-dind'

    // helper to run a single-stage pod using a resource YAML
    def runWithPod = { String resourcePath, Closure body ->
        def yaml = libraryResource(resourcePath)
        if (!yaml) {
            error "Pod template resource not found: ${resourcePath}"
        }
        podTemplate(label: "universal-pipeline-${env.BUILD_ID}-${resourcePath.replaceAll('[^a-zA-Z0-9]','')}", yaml: yaml) {
            node("universal-pipeline-${env.BUILD_ID}-${resourcePath.replaceAll('[^a-zA-Z0-9]','')}") {
                body()
            }
        }
    }

    node("universal-pipeline-${env.BUILD_ID}") {
            try {
                stage('Load GlobalConfig') {
                    if (params.GLOBAL_CONFIG_REPO) {
                        withCredentials([usernamePassword(credentialsId: 'github-credentials', usernameVariable: 'GIT_USER', passwordVariable: 'GIT_TOKEN')]) {
                            sh "rm -rf global-config || true"
                            sh "git clone https://${GIT_USER}:${GIT_TOKEN}@github.com/${params.GLOBAL_CONFIG_REPO}.git global-config || true"
                        }
                    } else {
                        echo 'No GLOBAL_CONFIG_REPO provided; skipping.'
                    }
                }

                stage('Environment Validation') {
                    script {
                        try {
                            if (fileExists('global-config/config/environments.yaml')) {
                                def envCfg = readYaml file: 'global-config/config/environments.yaml'
                                if (!envCfg.environments[envName]) {
                                    error "Environment ${envName} is not defined in global-config"
                                }
                                echo "Environment ${envName} validated"
                            } else {
                                echo 'No global-config environments.yaml found; skipping validation.'
                            }
                        } catch (e) {
                            echo "Environment validation failed: ${e}"
                            throw e
                        }
                    }
                }

                stage('Branch Validation') {
                    script {
                        def allowedPattern = params.BRANCH_ALLOW_REGEX ?: '^(main|master|develop|release\/.*|feature\/.*)'
                        if (!(branch ==~ /${allowedPattern}/)) {
                            error "Branch ${branch} is not allowed by policy (${allowedPattern})"
                        }
                        echo "Branch ${branch} passes validation"
                    }
                }

                stage('AWS Role Assumption') {
                    script {
                        if (params.AWS_ROLE_ARN) {
                            withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'aws-credentials']]) {
                                // Attempt to assume role using AWS CLI
                                sh '''#!/bin/bash
                                set -e
                                CREDS_JSON=$(aws sts assume-role --role-arn "${AWS_ROLE_ARN}" --role-session-name jenkins-session --duration-seconds 900)
                                export AWS_ACCESS_KEY_ID=$(echo "$CREDS_JSON" | jq -r '.Credentials.AccessKeyId')
                                export AWS_SECRET_ACCESS_KEY=$(echo "$CREDS_JSON" | jq -r '.Credentials.SecretAccessKey')
                                export AWS_SESSION_TOKEN=$(echo "$CREDS_JSON" | jq -r '.Credentials.SessionToken')
                                echo "Assumed role ${AWS_ROLE_ARN}"
                                '''
                            }
                        } else {
                            echo 'No AWS_ROLE_ARN provided; skipping role assumption.'
                        }
                    }
                }

                stage('Code Checkout') {
                    // Run checkout inside a lightweight generic pod so workspace is set
                    runWithPod('k8s/sonar-pod.yaml') {
                        if (repoUrl) {
                            gitClone(url: repoUrl, branch: branch)
                        } else {
                            checkout scm
                        }
                    }
                }

                stage('Security Scanning (IaC)') {
                    runWithPod('k8s/checkov-pod.yaml') {
                        checkovScanner(directory: '.')
                    }
                }

                stage('SBOM (SCA)') {
                    runWithPod('k8s/sbom-pod.yaml') {
                        sh 'syft . -o cyclonedx-json=sbom.json || true'
                    }
                }

                stage('Code Quality (SAST)') {
                    runWithPod('k8s/sonar-pod.yaml') {
                        sonarqubeScanner(projectKey: "${env.JOB_NAME}-${env.BUILD_NUMBER}", host: env.SONAR_HOST, tokenCred: 'sonar-token')
                    }
                }

                stage('Docker Build & Image Scan') {
                    runWithPod('k8s/docker-pod.yaml') {
                        def imageTag = params.IMAGE_TAG ?: "${env.BUILD_ID}"
                        def imageName = params.IMAGE_NAME ?: "${env.JOB_NAME}:${imageTag}"

                        sh "docker build -t ${imageName} . || true"

                        // run trivy in docker pod by calling trivy via docker image (fallback)
                        sh "docker run --rm aquasec/trivy:0.45.0 image --exit-code 1 --severity HIGH ${imageName} || true"

                        if (params.REGISTRY_URL) {
                            sh "docker tag ${imageName} ${params.REGISTRY_URL}/${imageName} || true"
                            withCredentials([usernamePassword(credentialsId: 'registry-credentials', usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
                                sh "echo ${REG_PASS} | docker login ${params.REGISTRY_URL} -u ${REG_USER} --password-stdin || true"
                                sh "docker push ${params.REGISTRY_URL}/${imageName} || true"
                            }
                        } else {
                            echo 'No REGISTRY_URL provided; skipping push.'
                        }
                    }
                }

                stage('Push Artifact / Image') {
                    echo 'Artifact and image push handled in previous stage if configured.'
                }

            } catch (err) {
                currentBuild.result = 'FAILURE'
                throw err
            } finally {
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
