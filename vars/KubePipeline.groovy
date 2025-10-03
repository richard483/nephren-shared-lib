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
                    prepareKubernetesDeployment(CONTAINER_NAME, DOCKER_IMAGE, APP_PORT, EXTERNAL_ENDPOINTS_IP, KUBE_NODEPORT)
                    sh """
                        /snap/bin/microk8s kubectl delete deployment ${CONTAINER_NAME} --ignore-not-found
                        /snap/bin/microk8s kubectl delete service ${CONTAINER_NAME} --ignore-not-found

                        /snap/bin/microk8s kubectl create configmap ${CONTAINER_NAME}-config --from-literal=key=value --dry-run=client -o yaml | /snap/bin/microk8s kubectl apply -f -
                        /snap/bin/microk8s kubectl create secret generic ${CONTAINER_NAME}-secret --from-literal=key=value --dry-run=client -o yaml | /snap/bin/microk8s kubectl apply -f -

                        echo "Deployment YAML:"
                        cat deployment.yaml

                        /snap/bin/microk8s kubectl apply -f deployment.yaml
                        /snap/bin/microk8s kubectl set env deployment/${CONTAINER_NAME} --from=configmap/${CONTAINER_NAME}-config
                        echo "Pod status:"
                        /snap/bin/microk8s kubectl get pods -l app=${CONTAINER_NAME}
                        echo "Checking for pod issues:"
                        /snap/bin/microk8s kubectl describe pods -l app=${CONTAINER_NAME}
                    """
                }
            }

            stage('Access Information') {
                steps {
                    sh """
                        echo "Waiting for pod to be ready..."
                        /snap/bin/microk8s kubectl wait --for=condition=ready pod -l app=${CONTAINER_NAME} --timeout=60s || true
                        echo "Service details:"
                        /snap/bin/microk8s kubectl get service ${CONTAINER_NAME}
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
        }
    }
}
