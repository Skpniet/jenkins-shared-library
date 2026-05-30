def notify(Map config = [:]) {
    def recipients = config.recipients ?: env.NOTIFY_EMAIL ?: 'devops@example.com'
    def status = config.status ?: currentBuild.currentResult

    echo "Sending notification for status: ${status}"

    try {
        mail(
            to: recipients,
            subject: "Jenkins Pipeline ${env.JOB_NAME} #${env.BUILD_NUMBER}: ${status}",
            body: "Build URL: ${env.BUILD_URL}\nStatus: ${status}\nJob: ${env.JOB_NAME}\n"
        )
    } catch (all) {
        echo "Mail step failed or mail plugin not configured. Status: ${status}"
    }
}
return this
