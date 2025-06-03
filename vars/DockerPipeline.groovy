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
                            def projectVersion = sh(script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()
                            echo "Current project version: ${projectVersion}"

                            def (major, minor, patch) = projectVersion.tokenize('.')

                            def newPatch = patch.toInteger() + 1
                            def newVersion = "${major}.${minor}.${newPatch}"

                            echo "New project version will be: ${newVersion}"

                            sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"

                            def branch_name = env.BRANCH_NAME

                            withCredentials([gitUsernamePassword(credentialsId: 'my-credentials-id',gitToolName: 'git-tool')]) {
                                sh 'git add pom.xml'
                                sh "git commit -m 'Bump version to ${newVersion}'"
                                sh 'git push origin ${branch_name}'
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
