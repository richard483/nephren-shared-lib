import com.nephren.DockerUtils

def call() {
    def utils = new DockerUtils(this)
    utils.removeDanglingImages()
}
