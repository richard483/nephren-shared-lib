import com.nephren.DockerUtils

def call(String containerName) {
    def utils = new DockerUtils(this)
    utils.stopAndRemove(containerName)
}
