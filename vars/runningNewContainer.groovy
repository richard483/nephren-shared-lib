def call(String appPort, String containerName, String dockerImage, String envFile, String networkName) {
    echo 'Running new container...'

    def runCommand = 'docker run -d'

    if (networkName != null && !networkName.isEmpty()) {
        runCommand += " --net ${networkName}"
    }

    if (appPort != null && !appPort.isEmpty()) {
        runCommand += " -p 0.0.0.0:${appPort}:${appPort}"
    }

    if (envFile != null && !envFile.isEmpty()) {
        withCredentials([file(credentialsId: envFile, variable: 'secretFile')]) {
            String envContent = sh(
                    script: 'cat $secretFile',
                    returnStdout: true
            ).trim()

            def envs = envContent.split('\n')

            envs.each { env ->
                runCommand += " -e \"${env.trim()}\""
            }
        }
    }

    runCommand += " --restart unless-stopped --name ${containerName} ${dockerImage}"

    sh runCommand
}
