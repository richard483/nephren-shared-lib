def call(String dockerImage, Map args) {
    sh getDockerImageBuildCommand(dockerImage, args)
}
