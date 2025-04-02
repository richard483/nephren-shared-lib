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
        script {
            // Explicitly pass variables to shell context
            def containerName = "${CONTAINER_NAME}"
            def dockerImage = "${DOCKER_IMAGE}"
            def appPort = "${APP_PORT}"
            def clusterPort = "${CLUSTER_PORT}"
            def clusterIP = "${CLUSTER_IP}"

            sh """#!/bin/bash
                # Debug: Show all variables
                echo "CONTAINER_NAME: ${containerName}"
                echo "DOCKER_IMAGE: ${dockerImage}"
                echo "APP_PORT: ${appPort}"
                echo "CLUSTER_PORT: ${clusterPort}"
                echo "CLUSTER_IP: ${clusterIP}"

                # Set Minikube Docker environment
                eval \$(minikube docker-env)
                
                # Delete resources with proper variable substitution
                kubectl delete deployment ${containerName} --ignore-not-found=true
                kubectl delete service ${containerName} --ignore-not-found=true

                # Create ConfigMap
                kubectl create configmap ${containerName}-config \\
                    --from-literal=key=value \\
                    --dry-run=client -o yaml | kubectl apply -f -

                # Create Secret
                kubectl create secret generic ${containerName}-secret \\
                    --from-literal=key=value \\
                    --dry-run=client -o yaml | kubectl apply -f -

                # Build Docker image
                docker build -t ${dockerImage} .

                # Generate deployment manifest
                cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${containerName}
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ${containerName}
  template:
    metadata:
      labels:
        app: ${containerName}
    spec:
      containers:
      - name: ${containerName}
        image: ${dockerImage}
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: ${appPort}
        envFrom:
        - configMapRef:
            name: ${containerName}-config
        - secretRef:
            name: ${containerName}-secret
---
apiVersion: v1
kind: Service
metadata:
  name: ${containerName}
spec:
  type: NodePort
  clusterIP: ${clusterIP}
  selector:
    app: ${containerName}
  ports:
    - port: ${appPort}
      targetPort: ${appPort}
      nodePort: ${clusterPort}
EOF

                # Apply deployment
                kubectl apply -f deployment.yaml

                # Verify deployment
                kubectl rollout status deployment/${containerName}
            """
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
