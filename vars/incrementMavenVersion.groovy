import com.nephren.MavenVersioner

def call() {
    def versioner = new MavenVersioner(this)
    versioner.increment()
}
