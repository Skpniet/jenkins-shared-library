def call(Map config = [:]) {
    def repoUrl = config.url ?: env.GIT_URL
    def branch = config.branch ?: 'main'

    if (!repoUrl) {
        error 'gitClone: repo URL not provided'
    }

    checkout([$class: 'GitSCM', branches: [[name: branch]], userRemoteConfigs: [[url: repoUrl]]])
}
return this
