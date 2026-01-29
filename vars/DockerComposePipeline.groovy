def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def PROJECT_NAME = pipelineParams.get('projectName')
    def COMPOSE_FILE = pipelineParams.get('composeFile', 'docker-compose.yml')
    def ENV_VARIABLES = pipelineParams.get('envVariables')

    pipeline {
        agent any
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithScm()
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
        }
    }
}
