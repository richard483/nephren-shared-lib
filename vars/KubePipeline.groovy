def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def DOCKER_IMAGE = pipelineParams.get('dockerImage')
    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_PORT = pipelineParams.get('appPort')
    def NETWORK_NAME = pipelineParams.get('networkName')

    pipeline {
        agent any
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithScm()
                }
            }

            stage('Build Docker Image') {
                steps {
                    sh 'eval $(minikube docker-env)'
                    buildDockerImage(DOCKER_IMAGE, pipelineParams.get('buildArgs'))
                }
            }

            stage('Deploy Application to Kubernetes') {
                steps {
                    // Create or update the ConfigMap
                    sh "kubectl create configmap ${CONTAINER_NAME}-config --from-literal=key=value --dry-run=client -o yaml | kubectl apply -f -"

                    // Deploy the application with the ConfigMap mounted and imagePullPolicy: Never
                    sh """
                        kubectl create deployment ${CONTAINER_NAME} --image=${DOCKER_IMAGE} --dry-run=client -o yaml > deployment.yaml
                        sed -i 's/        image: ${DOCKER_IMAGE}/        image: ${DOCKER_IMAGE}\\n        imagePullPolicy: Never/' deployment.yaml
                        kubectl apply -f deployment.yaml
                        rm deployment.yaml
                        kubectl set env deployment/${CONTAINER_NAME} --from=configmap/${CONTAINER_NAME}-config
                    """
                    sh "kubectl expose deployment ${CONTAINER_NAME} --type=NodePort --port=${APP_PORT}"
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
