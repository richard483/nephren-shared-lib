def call(String agentLabel, body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    pipeline {
        agent any

        environment {
            DOCKER_IMAGE = pipelineParams.get('dockerImage')
            CONTAINER_NAME = pipelineParams.get('projectName')
            APP_PORT = pipelineParams.get('appPort')
        }

        node {
            stage('Echo Parameters') {
                agent { label "${agentLabel}" }
                steps {
                    sh "env | sort"
                    pipelineParams.each { key, value ->
                        echo "${key}: ${value}"
                    }
                }
            }
            stages {
                stage('Checkout Code') {
                    checkoutWithScm()
                }

                stage('Build Docker Image') {
                    buildDockerImage(DOCKER_IMAGE, pipelineParams.get('buildArgs'))
                }

                stage('Deploy Application') {
                    stoppingAndRemovingContainer(CONTAINER_NAME)
                    runningNewContainer(APP_PORT, CONTAINER_NAME, DOCKER_IMAGE)
                }

                stage('Removing Dangling Images') {
                    removeDanglingImages()
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
