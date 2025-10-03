import com.nephren.KubernetesUtils

def call(String containerName, String dockerImage, String appPort, String externalEndpointIp, String kubeNodePort) {
    def k8s = new KubernetesUtils(this)
    k8s.prepareDeploymentYaml(containerName, dockerImage, appPort, externalEndpointIp, kubeNodePort)
}
