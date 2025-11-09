import com.nephren.GitUtils

def call(Integer checkoutTimeout, String repoUrl, String branch, String credentialsId) {
    def utils = new GitUtils(this)
    utils.checkoutWithCredential(checkoutTimeout: checkoutTimeout, repoUrl: repoUrl, branch: branch, credentialsId: credentialsId)
}
