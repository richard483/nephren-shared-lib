def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def PROJECT_NAME = pipelineParams.get('projectName')
    def COMPOSE_FILE = pipelineParams.get('composeFile', 'docker-compose.yml')
    def ENV_VARIABLES = pipelineParams.get('envVariables')
    def AGENT_LABEL = pipelineParams.get('agentLabel', 'any')

    def gitConfig = pipelineParams.get('gitConfig', [:])
    def CHECKOUT_TIMEOUT = gitConfig.get('checkoutTimeout', 10)
    def REPO_URL = gitConfig.get('repoUrl', '')
    def BRANCH = gitConfig.get('branch', 'main')
    def CREDENTIALS_ID = gitConfig.get('credentialsId', null)

    pipeline {
        agent { label AGENT_LABEL }
        options {
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5', fileSizeLimit: '10MB'))
            timeout(time: 30, unit: 'MINUTES')
            timestamps()
            skipDefaultCheckout()
            disableConcurrentBuilds()
        }
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithCredential(CHECKOUT_TIMEOUT, REPO_URL, BRANCH, CREDENTIALS_ID)
                }
            }

            stage('Compose Up') {
                steps {
                    dockerComposeUp(COMPOSE_FILE, PROJECT_NAME, ENV_VARIABLES)
                }
            }

            stage('Removing Dangling Images') {
                steps {
                    removingDanglingImage()
                }
            }
        }
        post {
            success {
                echo 'Pipeline succeeded!'
            }
            failure {
                echo 'Pipeline failed.'
            }
            always {
                cleanWs()
            }
        }
    }
}
