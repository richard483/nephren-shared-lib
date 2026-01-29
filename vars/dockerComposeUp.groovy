import com.nephren.DockerComposeUtils

def call(String composeFile, String projectName, def envVariables = null) {
    def utils = new DockerComposeUtils(this)
    utils.composeUp(composeFile, projectName, envVariables)
}
