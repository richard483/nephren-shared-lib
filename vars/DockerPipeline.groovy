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

                            def rawOutput = sh(
                                script: 'mvn -B help:evaluate -Dexpression=project.version -q -DforceStdout', 
                                returnStdout: true
                            ).trim()
                            
                            // FIXED: Use Groovy's find operator =~ instead of Matcher
                            String projectVersion = (rawOutput =~ /\d+\.\d+\.\d+([.-][A-Za-z0-9]+)?/)?.find() ? it[0] : null
                            
                            if (!projectVersion) {
                                error "Could not extract valid version from: '${rawOutput}'"
                            }
                            
                            echo "Extracted version: '${projectVersion}'"
                            sh "mvn versions:set -DnewVersion=${projectVersion} -DgenerateBackupPoms=false"
                            echo "Project version updated to ${projectVersion} in pom.xml"

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
