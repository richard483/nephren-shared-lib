def call(String networkName) {
    def networkExists = sh(
            script: "docker network ls --filter name=^${NETWORK_NAME}\$ --format '{{.Name}}' | grep -q '^${NETWORK_NAME}\$'",
            returnStatus: true
    ) == 0

    if (!networkExists) {
        sh "docker network create ${NETWORK_NAME}"
    } else {
        echo "Network ${NETWORK_NAME} already exists."
    }
}
