import com.nephren.DockerUtils

def call(String dockerImage, Map args = [:]) {
    def utils = new DockerUtils(this)
    utils.buildImage(dockerImage, args)
}
