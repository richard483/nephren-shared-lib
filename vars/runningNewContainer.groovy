def call(String appPort, String containerName, String dockerImage, String envFile) {
    echo 'Running new container...'

    def runCommand = "docker run -d --name ${containerName} --restart unless-stopped ${dockerImage}"

    if (appPort != null && !appPort.isEmpty()) {
        runCommand += " -p ${appPort}:${appPort}"
    }

    if (envFile != null && !envFile.isEmpty()) {
        withCredentials([file(credentialsId: envFile, variable: 'secretFile')]) {
            String envContent = sh(
                    script: 'cat $secretFile',
                    returnStdout: true
            )

            envContent = envContent.split("\n")
            envContent.each { line ->
                runCommand += " -e ${line}"
            }

        }
    }

    sh runCommand
}
