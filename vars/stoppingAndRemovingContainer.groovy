def call(String containerName) {
    echo 'Stopping and removing old container if it exists...'
    sh """
        CONTAINER_ID=\$(docker ps -aq -f name=${containerName})
        if [ ! -z "\$CONTAINER_ID" ]; then
            docker stop \$CONTAINER_ID
            docker rm \$CONTAINER_ID
        else
            echo "No container found with name ${containerName}."
        fi
    """
}
