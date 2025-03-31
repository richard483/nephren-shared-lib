def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def DOCKER_IMAGE = pipelineParams.get('dockerImage')
    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_PORT = pipelineParams.get('appPort')
    def NETWORK_NAME = pipelineParams.get('networkName')
    def CLUSTER_IP= pipelineParams.get('clusterIP')
    def CLUSTER_PORT= pipelineParams.get('clusterPort')

    pipeline {
        agent any
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithScm()
                }
            }

            stage('Build and Deploy to Kubernetes') {
                steps {
                    // Create a single shell script to ensure environment consistency
                    sh """
                        # Explicitly set and verify Minikube Docker environment
                        eval \$(minikube docker-env)
                        echo "Current Docker context:"
                        docker info | grep "Name:"
                        
                        # Build the image
                        echo "Building image: ${DOCKER_IMAGE}"
                        docker build -t ${DOCKER_IMAGE} .
                        
                        # Verify image exists
                        echo "Verifying image exists:"
                        docker images ${DOCKER_IMAGE} --format "{{.Repository}}:{{.Tag}}"
                        
                        # Delete existing resources
                        kubectl delete deployment ${CONTAINER_NAME} --ignore-not-found
                        kubectl delete service ${CONTAINER_NAME} --ignore-not-found
                        
                        # Create ConfigMap
                        kubectl create configmap ${CONTAINER_NAME}-config --from-literal=key=value --dry-run=client -o yaml | kubectl apply -f -
                        
                        # Create Secret
                        kubectl create secret generic ${CONTAINER_NAME}-secret --from-literal=key=value --dry-run=client -o yaml | kubectl apply -f -

                        # Create deployment YAML
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
        envFrom:
        - configMapRef:
          name: ${CONTAINER_NAME}-config
        - secretRef:
          name: ${CONTAINER_NAME}-secret
        ports:
        - containerPort: ${APP_PORT}
---
apiVersion: v1
kind: Service
metadata:
  name: ${CONTAINER_NAME}
spec:
    type: NodePort
    clusterIP: ${CLUSTER_IP}
    selector:
        app: ${CONTAINER_NAME}
    ports:
      - port: ${APP_PORT}
        targetPort: ${APP_PORT}
        nodePort: ${CLUSTER_PORT}
EOF
                        
                        # Debug: Show the YAML
                        echo "Deployment YAML:"
                        cat deployment.yaml
                        
                        # Apply the deployment
                        kubectl apply -f deployment.yaml
                        
                        # Set environment variables from ConfigMap
                        kubectl set env deployment/${CONTAINER_NAME} --from=configmap/${CONTAINER_NAME}-config
                        
                        # Verify pod status
                        echo "Pod status:"
                        kubectl get pods -l app=${CONTAINER_NAME}
                        
                        # Debug pod issues if not running
                        echo "Checking for pod issues:"
                        kubectl describe pods -l app=${CONTAINER_NAME}
                    """
                }
            }

            stage('Access Information') {
                steps {
                    sh """
                        # Wait for pod to be ready (timeout after 60 seconds)
                        echo "Waiting for pod to be ready..."
                        kubectl wait --for=condition=ready pod -l app=${CONTAINER_NAME} --timeout=60s || true
                        
                        # Get service information
                        echo "Service details:"
                        kubectl get service ${CONTAINER_NAME}
                        
                        # Get NodePort
                        NODE_PORT=\$(kubectl get service ${CONTAINER_NAME} -o jsonpath='{.spec.ports[0].nodePort}')
                        
                        # Get Minikube IP
                        MINIKUBE_IP=\$(minikube ip)
                        
                        echo "--------------------------------------"
                        echo "Service is accessible at: http://\$MINIKUBE_IP:\$NODE_PORT"
                        echo "Or run: minikube service ${CONTAINER_NAME}"
                        echo "Or run: kubectl port-forward service/${CONTAINER_NAME} ${APP_PORT}:${APP_PORT}"
                        echo "--------------------------------------"
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
