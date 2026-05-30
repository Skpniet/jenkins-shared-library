def call(Map config = [:]) {
    def url = config.url ?: env.DEPTRACK_URL
    def apiKey = config.apiKey ?: env.DEPTRACK_API_KEY

    if (!apiKey) {
        error 'DEPTRACK_API_KEY is not set. Configure the Dependency-Track API key in Jenkins credentials or environment.'
    }

    sh '''#!/bin/bash
        if ! command -v syft >/dev/null 2>&1; then
            echo 'syft is not installed; please install syft for SBOM generation.'
            exit 1
        fi

        syft . -o cyclonedx-json > sbom.json
        curl -X PUT "${url}/api/v1/bom" \
            -H "X-Api-Key: ${apiKey}" \
            -H 'Content-Type: application/json' \
            --data-binary @sbom.json
    '''
}
return this
