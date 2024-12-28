def call(String appPort, String containerName, String dockerImage, String envFile) {
    echo 'Running new container...'

    def runCommand = "docker run -d --name ${containerName} --restart unless-stopped ${dockerImage}"

    if (appPort != null && !appPort.isEmpty()) {
        runCommand += " -p ${appPort}:${appPort}"
    }

    if (envFile != null && !envFile.isEmpty()) {
        withCredentials([file(credentialsId: envFile, variable: 'secretFile')]) {
            def envContent = readFile(file: secretFile)
            envContent.eachLine { line ->
                runCommand += " -e ${line}"
            }
        }
    }

    sh runCommand
}
