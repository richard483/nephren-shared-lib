import com.nephren.DockerUtils

def call(String appPort, String containerName, String dockerImage, String envFile, String networkName, String volumeDriver, def envVariables = null) {
    def utils = new DockerUtils(this)
    utils.runContainer(appPort, containerName, dockerImage, envFile, networkName, volumeDriver, envVariables)
}
