def call(String appPort, String containerName, String dockerImage) {
    echo 'Running new container...'

    if (appPort == null || appPort.isEmpty()) {
        sh "docker run -d --name ${containerName} --restart unless-stopped ${dockerImage}"
        return
    }

    sh "docker run -d -p ${appPort}:${appPort} --name ${containerName} --restart unless-stopped ${dockerImage}"
}
