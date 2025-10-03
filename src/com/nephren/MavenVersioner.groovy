package com.nephren

import java.util.regex.Matcher
import java.util.regex.Pattern

class MavenVersioner implements Serializable {
    def script

    MavenVersioner(script) {
        this.script = script
    }

    @NonCPS
    private String extractVersionWithRegex(String rawOutput) {
        Pattern versionPattern = Pattern.compile("(\\d+\\.\\d+\\.\\d+([.-][A-Za-z0-9]+)*)")
        Matcher matcher = versionPattern.matcher(rawOutput)
        if (matcher.find()) {
            return matcher.group(1)
        }
        return ""
    }

    def increment() {
        script.echo "Detected Maven project. Incrementing version..."
        def rawOutput = script.sh(script: 'mvn -B help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true).trim()

        String projectVersion = extractVersionWithRegex(rawOutput)

        if (projectVersion.isEmpty()) {
            script.error "Could not extract a valid version string from raw output: '${rawOutput}'"
        }

        def versionParts = projectVersion.tokenize('-')
        def baseVersion = versionParts[0]
        def snapshotSuffix = versionParts.size() > 1 ? "-${versionParts[1]}" : ""

        if (!baseVersion.matches("^\\d+\\.\\d+\\.\\d+$")) {
            script.error "Extracted base version '${baseVersion}' is not in X.Y.Z format. Full extracted version was '${projectVersion}'"
        }
        def (major, minor, patch) = baseVersion.tokenize('.')

        if (!major.isNumber() || !minor.isNumber() || !patch.isNumber()) {
            script.error "Failed to parse major/minor/patch from baseVersion '${baseVersion}'. Original projectVersion was '${projectVersion}'"
        }

        def newPatch = patch.toInteger() + 1
        def newVersion = "${major}.${minor}.${newPatch}${snapshotSuffix}"

        script.sh "mvn versions:set -DnewVersion=${newVersion} -DgenerateBackupPoms=false"
        script.echo "Project version updated to ${newVersion} in pom.xml"

        def branch_name = script.env.BRANCH_NAME
        if (branch_name == null || branch_name.isEmpty()) {
            if (script.scm != null && script.scm.branches != null && !script.scm.branches.isEmpty()) {
                branch_name = script.scm.branches[0].name
                if (branch_name.startsWith("*/")) {
                    branch_name = branch_name.substring(2) // Remove "*/"
                } else if (branch_name.startsWith("refs/heads/")) {
                    branch_name = branch_name.substring("refs/heads/".length())
                }
            } else {
                script.error "Could not determine branch name. SCM information unavailable or env.BRANCH_NAME is not set."
            }
        }

        script.withCredentials([script.gitUsernamePassword(credentialsId: '14c17322-a8a2-4bc2-9a47-34d4ff8c148b', gitToolName: 'git-tool')]) {
            script.sh 'git config --global user.email ""'
            script.sh 'git config --global user.name "JENKINS"'
            script.sh 'git add pom.xml'
            script.sh "git commit -m 'JENKINS: Bump version to ${projectVersion}'"
            script.sh "git push origin HEAD:${branch_name}"
        }
    }
}
