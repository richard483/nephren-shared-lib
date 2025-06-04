import java.util.regex.Matcher
import java.util.regex.Pattern

@NonCPS
String extractVersionWithRegex(String rawOutput) {
    Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+([.-][A-Za-z0-9]+)*)")
    Matcher matcher = versionPattern.matcher(rawOutput)
    if (matcher.find()) {
        return matcher.group(1)
    }
    return ""
}

def call() {

    if (APP_TYPE == 'maven') {
        echo "Detected Maven project. Incrementing version..."
        def rawOutput = sh(script: 'mvn -B help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()

        String projectVersion = extractVersionWithRegex(rawOutput)

        if (projectVersion.isEmpty()) {
            error "Could not extract a valid version string from raw output: '${rawOutput}'"
        }

        def versionParts = projectVersion.tokenize('-')
        def baseVersion = versionParts[0]
        def snapshotSuffix = versionParts.size() > 1 ? "-${versionParts[1]}" : ""

        if (!baseVersion.matches("^\\d+\\.\\d+\\.\\d+\$")) {
            error "Extracted base version '${baseVersion}' is not in X.Y.Z format. Full extracted version was '${projectVersion}'"
        }
        def (major, minor, patch) = baseVersion.tokenize('.')

        if (!major.isNumber() || !minor.isNumber() || !patch.isNumber()) {
            error "Failed to parse major/minor/patch from baseVersion '${baseVersion}'. Original projectVersion was '${projectVersion}'"
        }

        def newPatch = patch.toInteger() + 1
        def newVersion = "${major}.${minor}.${newPatch}${snapshotSuffix}"

        sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
        echo "Project version updated to ${newVersion} in pom.xml"

        def branch_name = env.BRANCH_NAME
        if (branch_name == null || branch_name.isEmpty()) {
            if (scm != null && scm.branches != null && !scm.branches.isEmpty()) {
                branch_name = scm.branches[0].name
                if (branch_name.startsWith("*/")) {
                    branch_name = branch_name.substring(2) // Remove "*/"
                } else if (branch_name.startsWith("refs/heads/")) {
                    branch_name = branch_name.substring("refs/heads/".length())
                }
            } else {
                error "Could not determine branch name. SCM information unavailable or env.BRANCH_NAME is not set."
            }
        }

        withCredentials([gitUsernamePassword(credentialsId: '14c17322-a8a2-4bc2-9a47-34d4ff8c148b', gitToolName: 'git-tool')]) {
            sh 'git config --global user.email "richard.william483@gmail.com"'
            sh 'git config --global user.name "richard483"'
            sh 'git add pom.xml'
            sh "git commit -m 'JENKINS: Bump version to ${projectVersion}'"
            sh "git push origin HEAD:${branch_name}"
        }
    }
}
