def call(Map config = [:]) {
    def url = config.url ?: env.DEPTRACK_URL
    def apiKeyCred = config.apiKeyCred ?: 'deptrack-api-key'
    def sbomFile = config.sbom ?: 'sbom.json'

    if (!url) {
        error 'dependencyTrack: DEPTRACK_URL not set'
    }

    withCredentials([string(credentialsId: apiKeyCred, variable: 'DEPT_API_KEY')]) {
        sh "curl -X PUT \"${url}/api/v1/bom\" -H \"X-Api-Key: ${DEPT_API_KEY}\" -H 'Content-Type: application/json' --data-binary @${sbomFile} || true"
    }
}
return this
