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

    def gitConfig = pipelineParams.get('gitConfig', [:])
    def CHECKOUT_TIMEOUT = gitConfig.get('checkoutTimeout', 10)
    def REPO_URL = gitConfig.get('repoUrl', '')
    def BRANCH = gitConfig.get('branch', 'main')
    def CREDENTIALS_ID = gitConfig.get('credentialsId', null)

    pipeline {
        agent any
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithCredential(CHECKOUT_TIMEOUT, REPO_URL, BRANCH, CREDENTIALS_ID)
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
                    runningNewContainer(APP_PORT, CONTAINER_NAME, DOCKER_IMAGE, ENV_FILE, NETWORK_NAME, VOLUME_DRIVER)
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