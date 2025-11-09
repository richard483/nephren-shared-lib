package com.nephren

class GitUtils implements Serializable {
    def script

    GitUtils(script) {
        this.script = script
    }

    def checkoutWithCredential(Integer checkoutTimeout = 10, String repoUrl = '', String branch = 'main', String credentialsId = null) {
        
        if (!repoUrl) {
            error "Configuration error: 'repoUrl' must be provided to the checkout function."
        }

        def remoteConfig = [url: repoUrl]
        
        if (credentialsId) {
            remoteConfig.credentialsId = credentialsId
        }

        def scmCheckout = [
            $class: 'GitSCM',
            branches: [[name: branch]],
            userRemoteConfigs: [remoteConfig]
        ]

        script.echo "Checking out ${branch} from ${repoUrl} with a ${checkoutTimeout}-minute timeout..."
        
        timeout(time: checkoutTimeout, unit: 'MINUTES') {
            cleanWs() 
            
            checkout scmCheckout
        }
    }
}
