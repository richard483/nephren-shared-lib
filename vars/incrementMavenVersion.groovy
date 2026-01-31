import com.nephren.MavenVersioner

def call(String gitCredentialsId = null) {
    def versioner = new MavenVersioner(this, gitCredentialsId)
    versioner.increment()
}
