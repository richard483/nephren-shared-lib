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
                    // Delete the existing deployment and service
                    sh "kubectl delete deployment ${CONTAINER_NAME} --ignore-not-found"
                    sh "kubectl delete service ${CONTAINER_NAME} --ignore-not-found"

                    // Create or update the ConfigMap
                    sh "kubectl create configmap ${CONTAINER_NAME}-config --from-literal=key=value --dry-run=client -o yaml | kubectl apply -f -"

                    // Create a deployment with imagePullPolicy explicitly set to Never - with proper indentation
                    sh """
                        cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${CONTAINER_NAME}
  labels:
    app: ${CONTAINER_NAME}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${CONTAINER_NAME}
  template:
    metadata:
      labels:
        app: ${CONTAINER_NAME}
    spec:
      containers:
      - name: ${CONTAINER_NAME}
        image: ${DOCKER_IMAGE}
        imagePullPolicy: Never
        ports:
        - containerPort: ${APP_PORT}
EOF
                        cat deployment.yaml
                        kubectl apply -f deployment.yaml || (echo "Deployment failed, see deployment.yaml above for details" && exit 1)
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
