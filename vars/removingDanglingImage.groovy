def call() {
    echo 'Removing dangling images...'
    sh '''
        DANGLING_IMAGES=$(docker images -f "dangling=true" -q)
        if [ ! -z "$DANGLING_IMAGES" ]; then
            docker rmi -f $DANGLING_IMAGES
        else
            echo "No dangling images to remove."
        fi
    '''
}
