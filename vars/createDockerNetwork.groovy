def call(String networkName) {

    if (networkName == null || networkName.isEmpty()) {
        echo "No network name provided. Skipping network creation."
        return
    }

    def networkExists = sh(
            script: "docker network ls --filter name=^${networkName}\$ --format '{{.Name}}' | grep -q '^${networkName}\$'",
            returnStatus: true
    ) == 0

    if (!networkExists) {
        sh "docker network create ${networkName}"
    } else {
        echo "Network ${networkName} already exists."
    }
}
