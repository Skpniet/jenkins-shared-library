def gitClone(Map cfg = [:]) {
    def repoUrl = cfg.url ?: env.GIT_URL
    def branch = cfg.branch ?: 'main'
    if (!repoUrl) { error 'gitClone: repo URL not provided' }
    checkout([$class: 'GitSCM', branches: [[name: branch]], userRemoteConfigs: [[url: repoUrl]]])
}

// Sonar scanner helper
def runSonar(Map cfg = [:]) {
    def projectKey = cfg.projectKey ?: "${env.JOB_NAME}-${env.BUILD_NUMBER}"
    def host = cfg.host ?: env.SONAR_HOST
    def tokenCred = cfg.tokenCred ?: 'sonar-token'
    withCredentials([string(credentialsId: tokenCred, variable: 'SONAR_TOKEN')]) {
        sh "sonar-scanner -Dsonar.projectKey=${projectKey} -Dsonar.sources=. -Dsonar.host.url=${host} -Dsonar.login=${SONAR_TOKEN} || true"
    }
}

// Checkov helper
def runCheckov(Map cfg = [:]) {
    def dir = cfg.directory ?: '.'
    sh "checkov -d ${dir} -o json || true"
}

// SBOM helper (syft)
def runSbom(Map cfg = [:]) {
    def out = cfg.out ?: 'sbom.json'
    sh "syft . -o cyclonedx-json=${out} || true"
}

// Dependency-Track upload
def runDependencyTrack(Map cfg = [:]) {
    def url = cfg.url ?: env.DEPTRACK_URL
    def apiKeyCred = cfg.apiKeyCred ?: 'deptrack-api-key'
    def sbom = cfg.sbom ?: 'sbom.json'
    if (!url) { echo 'dependencyTrack: DEPTRACK_URL not set; skipping'; return }
    withCredentials([string(credentialsId: apiKeyCred, variable: 'DEPT_API_KEY')]) {
        sh "curl -X PUT \"${url}/api/v1/bom\" -H \"X-Api-Key: ${DEPT_API_KEY}\" -H 'Content-Type: application/json' --data-binary @${sbom} || true"
    }
}

// Docker build + scan + push
def buildAndPushImage(Map cfg = [:]) {
    def imageName = cfg.imageName ?: "${env.JOB_NAME}:${env.BUILD_NUMBER}"
    def registry = cfg.registry ?: params.REGISTRY_URL
    sh "docker build -t ${imageName} . || true"
    sh "docker run --rm aquasec/trivy:0.45.0 image --exit-code 1 --severity HIGH ${imageName} || true"
    if (registry) {
        def target = "${registry}/${imageName}"
        withCredentials([usernamePassword(credentialsId: 'registry-credentials', usernameVariable: 'REG_USER', passwordVariable: 'REG_PASS')]) {
            sh "echo ${REG_PASS} | docker login ${registry} -u ${REG_USER} --password-stdin || true"
            sh "docker tag ${imageName} ${target} || true"
            sh "docker push ${target} || true"
        }
    } else {
        echo 'No registry configured; skipping push.'
    }
}

// Notification
def sendNotify(Map cfg = [:]) {
    def recipients = cfg.recipients ?: env.NOTIFY_EMAIL ?: 'devops@example.com'
    def status = cfg.status ?: currentBuild.currentResult
    try {
        mail(to: recipients, subject: "Jenkins: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${status}", body: "${env.BUILD_URL}\nStatus: ${status}")
    } catch (e) { echo "notify failed: ${e}" }
}

// helper to run a stage inside pod yaml content
def runWithPodYaml(String yamlContent, Closure body) {
    podTemplate(yaml: yamlContent) {
        node(POD_LABEL) {
            body()
        }
    }
}

// Main entry
def call(Map params = [:]) {
    // basic params
    def repoUrl = params.GIT_URL ?: env.GIT_URL ?: ''
    def branch = params.GIT_BRANCH ?: env.BRANCH_NAME ?: 'main'
    def envName = params.ENVIRONMENT ?: 'development'

    try {
        stage('Branch Validation') {
            def allowedPattern = params.BRANCH_ALLOW_REGEX ?: '^(main|master|develop|release\/.*|feature\/.*)'
            if (!(branch ==~ /${allowedPattern}/)) { error "Branch ${branch} not allowed" }
            echo "Branch ${branch} ok"
        }

        stage('Checkout') {
            // run inside generic pod so workspace created
            def podYaml = readFile('ci/k8s/sonar-pod.yaml')
            podTemplate(yaml: podYaml) {
                node(POD_LABEL) {
                    if (repoUrl) { gitClone(url: repoUrl, branch: branch) } else { checkout scm }
                }
            }
        }

        stage('IaC Scan') {
            def podYaml = readFile('ci/k8s/checkov-pod.yaml')
            podTemplate(yaml: podYaml) {
                node(POD_LABEL) { runCheckov(directory: '.') }
            }
        }

        stage('SBOM') {
            def podYaml = readFile('ci/k8s/sbom-pod.yaml')
            podTemplate(yaml: podYaml) {
                node(POD_LABEL) { runSbom(out: 'sbom.json') }
            }
        }

        stage('SAST - Sonar') {
            def podYaml = readFile('ci/k8s/sonar-pod.yaml')
            podTemplate(yaml: podYaml) {
                node(POD_LABEL) { runSonar(projectKey: "${env.JOB_NAME}-${env.BUILD_NUMBER}", host: env.SONAR_HOST, tokenCred: 'sonar-token') }
            }
        }

        stage('Docker Build & Scan') {
            def podYaml = readFile('ci/k8s/docker-pod.yaml')
            podTemplate(yaml: podYaml) {
                node(POD_LABEL) { buildAndPushImage(imageName: params.IMAGE_NAME, registry: params.REGISTRY_URL) }
            }
        }

        stage('Upload SBOM') {
            runDependencyTrack(url: params.DEPTRACK_URL ?: env.DEPTRACK_URL, sbom: 'sbom.json')
        }

    } catch (err) {
        currentBuild.result = 'FAILURE'
        throw err
    } finally {
        sendNotify(status: currentBuild.currentResult)
    }
}

return this
