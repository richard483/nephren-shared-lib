import com.nephren.DockerUtils

def call(String networkName) {
    def utils = new DockerUtils(this)
    utils.createNetwork(networkName)
}
