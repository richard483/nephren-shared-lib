def call(String containerName, String dockerImage, String appPort, String externalEndpointIp) {
    sh """
        # Prepare deployment YAML
        cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${containerName}
  labels:
    app: ${containerName}
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
        image: localhost:32000/${dockerImage}
        imagePullPolicy: Always
        ports:
        - containerPort: ${appPort}
---
apiVersion: v1
kind: Service
metadata:
  name: ${containerName}
  annotations:
    metallb.universe.tf/loadBalancerIPs: ${externalEndpointIp}
spec:
    type: LoadBalancer
    selector:
        app: ${containerName}
    ports:
      - port: ${appPort}
        targetPort: ${appPort}
EOF
"""
}
