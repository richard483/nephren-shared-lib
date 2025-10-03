import com.nephren.DockerUtils

def call(String appPort, String containerName, String dockerImage, String envFile, String networkName) {
    def utils = new DockerUtils(this)
    utils.runContainer(appPort, containerName, dockerImage, envFile, networkName)
}
