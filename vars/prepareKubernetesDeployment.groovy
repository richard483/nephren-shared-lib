import com.nephren.KubernetesUtils

def call(String containerName, String dockerImage, String appPort, String externalEndpointIp, String kubeNodePort, String replicaCount, String healthCheckPath) {
    def k8s = new KubernetesUtils(this)
    k8s.prepareDeploymentYaml(containerName, dockerImage, appPort, externalEndpointIp, kubeNodePort, replicaCount, healthCheckPath)
}
