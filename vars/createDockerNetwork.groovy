def call(String networkName) {

    if (networkName == null || networkName.isEmpty()) {
        echo "No network name provided. Skipping network creation."
        return
    }

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
