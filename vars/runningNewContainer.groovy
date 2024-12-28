def call(String appPort, String containerName, String dockerImage, String envFile) {
    echo 'Running new container...'

    def runCommand = "docker run -d"

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
            def envs = envContent.split("\n")

            envs.each { env ->
                runCommand += " -e ${env}"
            }
        }
    }

    runCommand += " --name ${containerName} ${dockerImage}"

    sh runCommand
}
