def configMap = [
    tools: [
        sonarqube: [enabled: true, image: 'sonarsource/sonar-scanner-cli:4.7.0', tokenCred: 'sonar-token'],
        checkov:    [enabled: true, image: 'bridgecrew/checkov:2.0.607'],
        sbom:       [enabled: true, image: 'anchore/syft:1.12.0'],
        trivy:      [enabled: true, image: 'aquasec/trivy:0.45.0'],
        docker:     [enabled: true, image: 'docker:24-dind']
    ],

    environments: [
        development: [sonarqube_host: 'http://sonarqube:9000', deptrack_host: 'http://deptrack:8080'],
        staging:     [sonarqube_host: 'http://sonarqube-staging:9000', deptrack_host: 'http://deptrack-staging:8080'],
        production:  [sonarqube_host: 'http://sonarqube-prod:9000', deptrack_host: 'http://deptrack-prod:8080']
    ],

    podTemplates: [
        sonar:  'k8s/sonar-pod.yaml',
        checkov:'k8s/checkov-pod.yaml',
        sbom:   'k8s/sbom-pod.yaml',
        docker: 'k8s/docker-pod.yaml'
    ],

    defaultEnvironment: 'development'
]

def getConfig() { return configMap }
def tool(String name) { return configMap.tools[name] ?: [:] }
def pod(String name) { return configMap.podTemplates[name] }
def envConfig(String name) { return configMap.environments[name] ?: [:] }

def call() { return this }

return this
