def call(String appPort, String containerName, String dockerImage) {
    echo 'Running new container...'
    sh "docker run -d -p ${appPort}:${appPort} --name ${containerName} --restart unless-stopped ${dockerImage}"
}
