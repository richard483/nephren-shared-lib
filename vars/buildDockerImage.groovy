def call(String dockerImage, Map args) {
    echo 'Building Docker image...'
    def dockerBuildCommand = "docker build -t ${dockerImage}"

    args.each { key, value ->
        dockerBuildCommand += " --build-arg ${key}=${value}"
    }

    dockerBuildCommand += ' .'

    sh dockerBuildCommand
}
