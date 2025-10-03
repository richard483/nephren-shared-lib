# Nephren Shared Library

This repository provides a Jenkins Shared Library with reusable pipeline entrypoints in `vars/` and helper classes in `src/com/nephren`.

Include the library in your Jenkinsfile (name depends on how you register it in Jenkins):

```groovy
@Library('nephren-shared-lib') _
```

Examples

- Global Docker pipeline (build + run):

```groovy
GlobalPipeline() {
  dockerImage = 'my-org/my-app:latest'         // required
  projectName = 'my-app'                      // required (container/service name)
  appPort = '8080'                            // optional
  networkName = 'my-network'                  // optional
  envFile = 'jenkins-secret-id'               // optional (credentialsId of a secret file)
  buildArgs = [FOO: 'bar']                    // optional map of build args
}
```

- Kubernetes pipeline (build + deploy):

```groovy
KubePipeline() {
  dockerImage = 'my-org/my-app:latest'
  projectName = 'my-app'
  appPort = '8080'
  kubeNodePort = '30080'                      // optional nodePort for Service
  externalEndpointsIp = '10.0.0.50'           // MetalLB / loadBalancer IP(s)
}
```

- Maven increment-only pipeline:

```groovy
MavenIncrementVersionPipeline() {
  projectName = 'my-app'
  appType = 'maven'   // triggers Maven version increment when set
}
```

- Call individual helpers from a Jenkinsfile (examples):

```groovy
buildDockerImage('localhost:32000/my-app:1.2.3', [ARG1: 'value'])
prepareKubernetesDeployment('my-app','my-app:1.2.3','8080','10.0.0.50','30080')
stoppingAndRemovingContainer('my-app')
runningNewContainer('8080','my-app','localhost:32000/my-app:1.2.3','jenkins-secret-id','my-network')
createDockerNetwork('my-network')
removingDanglingImage()
incrementMavenVersion()
```

Notes
- `vars/` functions remain the public API for Jenkinsfiles. Implementation details live under `src/com/nephren` and can be unit tested.
- The examples assume the shared library is registered in Jenkins as `nephren-shared-lib`. Adjust `@Library(...)` accordingly.

