def call(Map config = [:]) {
    def directory = config.directory ?: '.'
    sh "checkov -d ${directory} -o json || true"
}
return this
