package com.nephren

class KubernetesUtils implements Serializable {
    def script

    KubernetesUtils(script) {
        this.script = script
    }

    def prepareDeploymentYaml(String containerName, String dockerImage, String appPort, String externalEndpointIp, String kubeNodePort, String replicaCount, String healthCheckPath) {
        
        def probeAndLifecycleYaml = ""
        
        if (healthCheckPath && !healthCheckPath.trim().isEmpty()) {
            
            probeAndLifecycleYaml = """
        readinessProbe:
          httpGet:
            path: ${healthCheckPath}
            port: ${appPort}
          initialDelaySeconds: 5
          periodSeconds: 10
        livenessProbe:
          httpGet:
            path: ${healthCheckPath}
            port: ${appPort}
          initialDelaySeconds: 30
          periodSeconds: 15
        lifecycle:
          preStop:
            exec:
              command: ["/bin/sh", "-c", "sleep 10"]"""
        }

        script.sh """
            # Prepare deployment YAML
            cat <<EOF > deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ${containerName}
  labels:
    app: ${containerName}
spec:
  replicas: ${replicaCount}
  selector:
    matchLabels:
      app: ${containerName}
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxUnavailable: ${(replicaCount.toInteger() + 1) / 2}
      maxSurge: 1
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
        ${probeAndLifecycleYaml}
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
        nodePort: ${kubeNodePort}
EOF
"""
    }
}