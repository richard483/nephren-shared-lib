def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def DOCKER_IMAGE = pipelineParams.get('dockerImage')
    def CONTAINER_NAME = pipelineParams.get('projectName')
    def APP_PORT = pipelineParams.get('appPort')
    def ENV_FILE = pipelineParams.get('envFile')
    def NETWORK_NAME = pipelineParams.get('networkName')
    def APP_TYPE = pipelineParams.get('appType') ?: 'default'

    pipeline {
        agent any
        stages {
            stage('Checkout Code') {
                steps {
                    checkoutWithScm()
                }
            }

            stage('Increment Version') {
                steps {
                    script {
                        if (APP_TYPE == 'maven') {
                            echo "Detected Maven project. Incrementing version..."

                        def rawOutput = sh(script: 'mvn -B help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                        echo "1. Raw output from mvn: '${rawOutput}'"
                        // Optional: Print bytes to see hidden characters
                        // echo "Raw output bytes: ${rawOutput.bytes.collect { String.format("%02X", it) }.join(' ')}"

                        // Attempt to clean ANSI SGR codes (like color codes)
                        // \x1B is ESC, \[ is literal [, [0-9;]* matches zero or more digits or semicolons, m is the terminator for SGR codes.
                        def projectVersion = rawOutput.replaceAll(/\x1B\[[0-9;]*m/, "")
                        echo "2. Project version after SGR cleaning: '${projectVersion}'"

                        // As a fallback, if the above isn't enough, try a more general ANSI code regex.
                        // This one looks for ESC [ followed by any parameters and a letter.
                        // Use this if "Attempt 1" still shows problems.
                        // projectVersion = projectVersion.replaceAll(/\u001B\[[;?\d]*[A-Za-z]/, "")
                        // echo "2b. Project version after general ANSI cleaning: '${projectVersion}'"


                        // Validate that the version string looks like a version now
                        if (!projectVersion.matches("^\\d+\\.\\d+\\.\\d+.*\$")) { // Allows for -SNAPSHOT etc. at the end
                            error "Cleaned project version '${projectVersion}' does not look like a valid version string. Raw was: '${rawOutput}'"
                        }
                        echo "3. Final project version for tokenization: '${projectVersion}'"


                        def versionParts = projectVersion.tokenize('-')
                        def baseVersion = versionParts[0]
                        def snapshotSuffix = versionParts.size() > 1 ? "-${versionParts[1]}" : ""

                        def (major, minor, patch) = baseVersion.tokenize('.')
                        // Ensure major, minor, patch are clean numbers
                        if (!major.isNumber() || !minor.isNumber() || !patch.isNumber()) {
                            error "Failed to parse major/minor/patch from baseVersion '${baseVersion}'. Original projectVersion was '${projectVersion}'"
                        }

                        def newPatch = patch.toInteger() + 1
                        def newVersion = "${major}.${minor}.${newPatch}${snapshotSuffix}"

                        echo "4. New project version calculated: '${newVersion}'" // Echo with quotes

                        // CRITICAL: newVersion MUST be clean before setting it in pom.xml
                        if (newVersion.contains("\u001B")) { // Check for any remaining ESC character
                            error "FATAL: Calculated newVersion '${newVersion}' still contains ANSI escape codes!"
                        }

                            echo "New project version will be: ${newVersion}"

                            sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"

                            def branch_name = env.BRANCH_NAME
                            if (branch_name == null || branch_name.isEmpty()) {
                                if (scm != null && scm.branches != null && !scm.branches.isEmpty()) {
                                    branch_name = scm.branches[0].name
                                    if (branch_name.startsWith("*/")) { // More robust check
                                        branch_name = branch_name.substring(2) // Remove "*/"
                                    } else if (branch_name.startsWith("refs/heads/")) {
                                        branch_name = branch_name.substring("refs/heads/".length())
                                    }
                                } else {
                                    error "Could not determine branch name. SCM information unavailable or env.BRANCH_NAME is not set."
                                }
                            }

                            withCredentials([gitUsernamePassword(credentialsId: '14c17322-a8a2-4bc2-9a47-34d4ff8c148b',gitToolName: 'git-tool')]) {
                                sh 'git config --global user.email "richard.william483@gmail.com"'
                                sh 'git config --global user.name "richard483"'
                                sh 'git add pom.xml'
                                sh "git commit -m 'JENKINS: Bump version to ${newVersion}'"
                                sh "git push origin HEAD:${branch_name}"
                            }
                        } 
                    }
                }
            }

            // stage('Build Docker Image') {
            //     steps {
            //         buildDockerImage(DOCKER_IMAGE, pipelineParams.get('buildArgs'))
            //     }
            // }

            // stage('Deploy Application') {
            //     steps {
            //         stoppingAndRemovingContainer(CONTAINER_NAME)
            //         createDockerNetwork(NETWORK_NAME)
            //         runningNewContainer(APP_PORT, CONTAINER_NAME, DOCKER_IMAGE, ENV_FILE, NETWORK_NAME)
            //     }
            // }

            // stage('Removing Dangling Images') {
            //     steps {
            //         removingDanglingImage()
            //     }
            // }
        }
        post {
            success {
                echo 'Pipeline succeeded!'
            }
            failure {
                echo 'Pipeline failed.'
            }
        }
    }
}
