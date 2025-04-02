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
    def builtImageCommand = getDockerImageBuildCommand(DOCKER_IMAGE, pipelineParams.get('buildArgs'))

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
        script {
            // Ensure consistent environment context
            withEnv(["DOCKER_IMAGE=${DOCKER_IMAGE}"]) {
                sh '''
                    # Set Minikube Docker environment
                    eval $(minikube docker-env)
                    echo "Current Docker context:"
                    docker info | grep "Name:"
                    
                    # Build image INSIDE Minikube's Docker context
                    docker build -t ${DOCKER_IMAGE} .  # Add your build arguments here
                    echo "Built images:"
                    docker images | grep "${DOCKER_IMAGE%:*}"
                    
                    # Cleanup previous deployment
                    kubectl delete deployment ${CONTAINER_NAME} --ignore-not-found
                    kubectl delete service ${CONTAINER_NAME} --ignore-not-found
                    
                    # Create ConfigMap and Secret
                    kubectl apply -f - <<EOF
apiVersion: v1
kind: ConfigMap
metadata:
  name: ${CONTAINER_NAME}-config
data:
  key: value
---
apiVersion: v1
kind: Secret
metadata:
  name: ${CONTAINER_NAME}-secret
type: Opaque
data:
  key: $(echo -n "value" | base64)
EOF

                    # Create deployment with proper environment references
                    cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${CONTAINER_NAME}
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
        imagePullPolicy: IfNotPresent  # Changed from Never
        ports:
        - containerPort: ${APP_PORT}
        envFrom:
        - configMapRef:
            name: ${CONTAINER_NAME}-config
        - secretRef:
            name: ${CONTAINER_NAME}-secret
---
apiVersion: v1
kind: Service
metadata:
  name: ${CONTAINER_NAME}
spec:
  type: NodePort
  selector:
    app: ${CONTAINER_NAME}
  ports:
    - port: ${APP_PORT}
      targetPort: ${APP_PORT}
      nodePort: ${CLUSTER_PORT}
EOF

                    # cat YAML
                    cat deployment.yaml
                    
                    # Apply configuration
                    kubectl apply -f deployment.yaml
                    
                    # Wait for deployment rollout
                    kubectl rollout status deployment/${CONTAINER_NAME} --timeout=2m
                    
                    # Verify resources
                    kubectl get deployment,svc,pod -l app=${CONTAINER_NAME}
                '''
            }
        }
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
