def call(String appPort, String containerName, String dockerImage, String envFile) {
    echo 'Running new container...'

    def runCommand = "docker run -d --name ${containerName} --restart unless-stopped ${dockerImage}"

    if (appPort != null && !appPort.isEmpty()) {
        runCommand += " -p ${appPort}:${appPort}"
    }

//    if (envFile != null && !envFile.isEmpty()) {
//        ENV_FILE = credentials(envFile)
//        runCommand += " --env-file ${ENV_FILE}"
//    }

    sh runCommand
}
