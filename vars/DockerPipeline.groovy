def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def DOCKER_IMAGE = pipelineParams.get('dockerImage')
    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_PORT = pipelineParams.get('appPort')
    def ENV_FILE = pipelineParams.get('envFile')
    def NETWORK_NAME = pipelineParams.get('networkName')
    def VOLUME_DRIVER = pipelineParams.get('volumeDriver')
    def ENV_VARIABLES = pipelineParams.get('envVariables')

    // Input validation
    if (!DOCKER_IMAGE?.trim()) {
        error "Required parameter 'dockerImage' is missing or empty"
    }
    if (!CONTAINER_NAME?.trim()) {
        error "Required parameter 'projectName' is missing or empty"
    }

    pipeline {
        agent any
        options {
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5'))
            timeout(time: 30, unit: 'MINUTES')
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

            stage('Build Docker Image') {
                steps {
                    buildDockerImage(DOCKER_IMAGE, pipelineParams.get('buildArgs'))
                }
            }

            stage('Deploy Application') {
                steps {
                    stoppingAndRemovingContainer(CONTAINER_NAME)
                    createDockerNetwork(NETWORK_NAME)
                    runningNewContainer(APP_PORT, CONTAINER_NAME, DOCKER_IMAGE, ENV_FILE, NETWORK_NAME, VOLUME_DRIVER, ENV_VARIABLES)
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
