def call(Map config = [:]) {
    def repoUrl = config.url ?: env.GIT_URL
    def branch = config.branch ?: 'main'

    if (!repoUrl) {
        error 'GIT_URL is not set. Please provide a valid repository URL.'
    }

    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [[url: repoUrl]]
    ])
}
return this
