package com.nephren

class DockerComposeUtils implements Serializable {
    def script

    DockerComposeUtils(script) {
        this.script = script
    }

    def composeUp(String composeFile, String projectName) {
        script.echo 'Running docker compose up...'

        def command = 'docker compose'

        if (composeFile != null && !composeFile.isEmpty()) {
            command += " -f ${composeFile}"
        }

        if (projectName != null && !projectName.isEmpty()) {
            command += " --project-name ${projectName}"
        }

        command += ' up -d --build'

        script.sh command
    }
}
