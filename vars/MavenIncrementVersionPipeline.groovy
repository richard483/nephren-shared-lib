def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_TYPE = pipelineParams.get('appType') ?: 'default'

    // Input validation
    if (!CONTAINER_NAME?.trim()) {
        error "Required parameter 'projectName' is missing or empty"
    }

    pipeline {
        agent any
        options {
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5', fileSizeLimit: '10MB'))
            timeout(time: 15, unit: 'MINUTES')
            timestamps()
            skipDefaultCheckout()
            disableConcurrentBuilds()
        }
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithScm()
                }
            }

            stage('Increment Version') {
                steps {
                    script {
                        if (APP_TYPE == 'maven') {
                            incrementMavenVersion()
                        }
                    }
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