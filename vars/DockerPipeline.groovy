import java.util.regex.Matcher
import java.util.regex.Pattern
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
                            // For deeper debugging of rawOutput if needed later:
                            // echo "Raw output bytes: ${rawOutput.bytes.collect { String.format('%02X', it) }.join(' ')}"

                            // --- Strategy: Extract the version string directly ---
                            String projectVersion = ""
                            // This regex looks for patterns like X.Y.Z, X.Y.Z-SNAPSHOT, X.Y.Z.RC1, etc.
                            Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+([.-][A-Za-z0-9]+)*)")
                            Matcher matcher = versionPattern.matcher(rawOutput)

                            if (matcher.find()) {
                                projectVersion = matcher.group(1) // group(1) gets the main captured version string
                            }

                            echo "2. Version extracted by regex: '${projectVersion}'"

                            if (projectVersion.isEmpty()) {
                                error "Could not extract a valid version string from raw output: '${rawOutput}'"
                            }
                            echo "3. Final project version for tokenization: '${projectVersion}'"

                            // --- The rest of your logic should now work with a clean projectVersion ---
                            def versionParts = projectVersion.tokenize('-')
                            def baseVersion = versionParts[0]
                            def snapshotSuffix = versionParts.size() > 1 ? "-${versionParts[1]}" : ""

                            // Double-check baseVersion before tokenizing by '.'
                            if (!baseVersion.matches("^\\d+\\.\\d+\\.\\d+\$")) {
                                error "Extracted base version '${baseVersion}' is not in X.Y.Z format. Full extracted version was '${projectVersion}'"
                            }
                            def (major, minor, patch) = baseVersion.tokenize('.')

                            if (!major.isNumber() || !minor.isNumber() || !patch.isNumber()) {
                                error "Failed to parse major/minor/patch from baseVersion '${baseVersion}'. Original projectVersion was '${projectVersion}'"
                            }

                            def newPatch = patch.toInteger() + 1
                            def newVersion = "${major}.${minor}.${newPatch}${snapshotSuffix}"

                            echo "4. New project version calculated: '${newVersion}'"

                            if (newVersion.contains("\u001B")) {
                                error "FATAL: Calculated newVersion '${newVersion}' still contains ANSI escape codes after extraction strategy!"
                            }

                            sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
                            echo "Project version updated to ${newVersion} in pom.xml"

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
                                sh "git commit -m 'JENKINS: Bump version to ${projectVersion}'"
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