package com.nephren

class DockerUtils implements Serializable {
    def script

    DockerUtils(script) {
        this.script = script
    }

    def buildImage(String dockerImage, Map args = [:]) {
        script.echo 'Building Docker image...'
        def dockerBuildCommand = "docker build -t ${dockerImage}"

        args.each { key, value ->
            dockerBuildCommand += " --build-arg ${key}=${value}"
        }

        dockerBuildCommand += ' .'

        script.sh dockerBuildCommand
    }

    def createNetwork(String networkName) {
        if (networkName == null || networkName.isEmpty()) {
            script.echo 'No network name provided. Skipping network creation.'
            return
        }

        def networkExists = script.sh(
                script: "docker network ls --filter name=^${networkName}\$ --format '{{.Name}}' | grep -q '^${networkName}\$'",
                returnStatus: true
        ) == 0

        if (!networkExists) {
            script.sh "docker network create ${networkName}"
        } else {
            script.echo "Network ${networkName} already exists."
        }
    }

    def runContainer(String appPort, String containerName, String dockerImage, String envFile, String networkName, String volumeDriver, def envVariables = null) {
        script.echo 'Running new container...'

        def runCommand = 'docker run -d'

        if (networkName != null && !networkName.isEmpty()) {
            runCommand += " --net ${networkName}"
        }

        if (volumeDriver != null && !volumeDriver.isEmpty()) {
            runCommand += " -v ${volumeDriver}"
        }

        if (appPort != null && !appPort.isEmpty()) {
            runCommand += " -p 0.0.0.0:${appPort}:${appPort}"
        }

        if (envFile != null && !envFile.isEmpty()) {
            script.withCredentials([script.file(credentialsId: envFile, variable: 'secretFile')]) {
                String envContent = script.sh(
                        script: 'cat $secretFile',
                        returnStdout: true
                ).trim()

                def envs = envContent.split('\n')

                envs.each { env ->
                    runCommand += " -e \"${env.trim()}\""
                }
            }
        }

        if (envVariables instanceof Map) {
            envVariables.each { key, value ->
                runCommand += " -e \"${key}=${value}\""
            }
        } else if (envVariables instanceof List) {
            envVariables.each { env ->
                runCommand += " -e \"${env.toString().trim()}\""
            }
        }

        runCommand += " --restart unless-stopped --name ${containerName} ${dockerImage}"

        script.sh runCommand
    }

    def stopAndRemove(String containerName) {
        script.echo 'Stopping and removing old container if it exists...'
        script.sh """
            CONTAINER_ID=\$(docker ps -aq -f name=${containerName})
            if [ ! -z "\$CONTAINER_ID" ]; then
                docker stop \$CONTAINER_ID
                docker rm \$CONTAINER_ID
            else
                echo "No container found with name ${containerName}."
            fi
        """
    }

    def removeDanglingImages() {
        script.echo 'Removing dangling images...'
        script.sh '''
            DANGLING_IMAGES=$(docker images -f "dangling=true" -q)
            if [ ! -z "$DANGLING_IMAGES" ]; then
                docker rmi -f $DANGLING_IMAGES
            else
                echo "No dangling images to remove."
            fi
        '''
    }
}
