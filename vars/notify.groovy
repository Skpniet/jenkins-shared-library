def call(Map config = [:]) {
    def recipients = config.recipients ?: env.NOTIFY_EMAIL ?: 'devops@example.com'
    def status = config.status ?: currentBuild.currentResult

    try {
        mail(to: recipients, subject: "Jenkins: ${env.JOB_NAME} #${env.BUILD_NUMBER} - ${status}", body: "${env.BUILD_URL}\nStatus: ${status}")
    } catch (e) {
        echo "notify: mail failed or plugin missing: ${e}"
    }
}
return this
