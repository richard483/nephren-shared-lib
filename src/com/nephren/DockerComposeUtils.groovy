package com.nephren

class DockerComposeUtils implements Serializable {
    def script

    DockerComposeUtils(script) {
        this.script = script
    }

    def composeUp(String composeFile, String projectName, def envVariables = null) {
        script.echo 'Running docker compose up...'

        def command = 'docker compose'
        def envFilePath = null

        if (composeFile != null && !composeFile.isEmpty()) {
            command += " -f ${composeFile}"
        }

        if (envVariables instanceof Map && !envVariables.isEmpty()) {
            envFilePath = '.compose.env'
            def lines = envVariables.collect { key, value -> "${key}=${value}" }.join('\n')
            script.writeFile(file: envFilePath, text: lines)
            command += " --env-file ${envFilePath}"
        } else if (envVariables instanceof List && !envVariables.isEmpty()) {
            envFilePath = '.compose.env'
            def lines = envVariables.collect { env -> env.toString().trim() }.join('\n')
            script.writeFile(file: envFilePath, text: lines)
            command += " --env-file ${envFilePath}"
        }

        if (projectName != null && !projectName.isEmpty()) {
            command += " --project-name ${projectName}"
        }

        command += ' up -d --build'

        try {
            script.sh command
        } finally {
            if (envFilePath != null) {
                script.sh "rm -f ${envFilePath}"
            }
        }
    }
}
