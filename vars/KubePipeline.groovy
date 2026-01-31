def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def DOCKER_IMAGE = pipelineParams.get('dockerImage')
    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_PORT = pipelineParams.get('appPort')
    def KUBE_NODEPORT = pipelineParams.get('kubeNodePort') ?: ''
    def EXTERNAL_ENDPOINTS_IP = pipelineParams.get('externalEndpointsIp')
    def REPLICA_COUNT = pipelineParams.get('replicaCount') ?: '1'
    def HEALTH_CHECK_PATH = pipelineParams.get('healthCheckPath') ?: ''
    def KUBECTL_PATH = pipelineParams.get('kubectlPath') ?: '/snap/bin/microk8s kubectl'

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
            buildDiscarder(logRotator(artifactDaysToKeepStr: '', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '5', fileSizeLimit: '10MB'))
            timeout(time: 45, unit: 'MINUTES')
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
                    sh "echo \"Building image: ${DOCKER_IMAGE}\""
                    buildDockerImage('localhost:32000/' + DOCKER_IMAGE, pipelineParams.get('buildArgs'))
                    sh """
                        echo "Verifying image exists:"
                        docker images ${DOCKER_IMAGE} --format "{{.Repository}}:{{.Tag}}"
                        docker push localhost:32000/${DOCKER_IMAGE}
                        """
                }
            }

            stage('Deploy to Kubernetes') {
                steps {
                    prepareKubernetesDeployment(CONTAINER_NAME, DOCKER_IMAGE, APP_PORT, EXTERNAL_ENDPOINTS_IP, KUBE_NODEPORT, REPLICA_COUNT, HEALTH_CHECK_PATH)
                    sh """
                        ${KUBECTL_PATH} delete deployment ${CONTAINER_NAME} --ignore-not-found
                        ${KUBECTL_PATH} delete service ${CONTAINER_NAME} --ignore-not-found

                        ${KUBECTL_PATH} create configmap ${CONTAINER_NAME}-config --from-literal=key=value --dry-run=client -o yaml | ${KUBECTL_PATH} apply -f -
                        ${KUBECTL_PATH} create secret generic ${CONTAINER_NAME}-secret --from-literal=key=value --dry-run=client -o yaml | ${KUBECTL_PATH} apply -f -

                        echo "Deployment YAML:"
                        cat deployment.yaml

                        ${KUBECTL_PATH} apply -f deployment.yaml
                        ${KUBECTL_PATH} set env deployment/${CONTAINER_NAME} --from=configmap/${CONTAINER_NAME}-config
                        echo "Pod status:"
                        ${KUBECTL_PATH} get pods -l app=${CONTAINER_NAME}
                        echo "Checking for pod issues:"
                        ${KUBECTL_PATH} describe pods -l app=${CONTAINER_NAME}
                    """
                }
            }

            stage('Access Information') {
                steps {
                    sh """
                        echo "Waiting for pod to be ready..."
                        ${KUBECTL_PATH} wait --for=condition=ready pod -l app=${CONTAINER_NAME} --timeout=60s || true
                        echo "Service details:"
                        ${KUBECTL_PATH} get service ${CONTAINER_NAME}
                    """
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
