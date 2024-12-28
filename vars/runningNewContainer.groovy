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

            sh "cat $secretFile"
            envContent = envContent.split("\n")
            sh "cat ${envContent.get(0)}"

        }
    }

    sh runCommand
}
