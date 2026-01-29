# Nephren Shared Library

This repository provides a Jenkins Shared Library with reusable pipeline entrypoints in `vars/` and helper classes in `src/com/nephren`.

Include the library in your Jenkinsfile (name depends on how you register it in Jenkins):

```groovy
@Library('nephren-shared-lib') _
```

Examples

- Script Docker pipeline (build + deploy with Git checkout):

```groovy
ScriptDockerPipeline() {
  dockerImage = 'my-org/my-app:latest'         // required
  projectName = 'my-app'                      // required (container/service name)
  appPort = '8080'                            // optional
  networkName = 'my-network'                  // optional
  envFile = 'jenkins-secret-id'               // optional (credentialsId of a secret file)
  envVariables = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  buildArgs = [FOO: 'bar']                    // optional map of build args
  volumeDriver = 'my-volume-driver'           // optional volume driver for container (example: '/var/lib/docker/volumes:my-volume/_data' on host)
  agentLabel = 'docker-node'                  // optional agent label (default: 'any')
  gitConfig = [                               // optional Git configuration
    repoUrl: 'https://github.com/org/repo.git',
    branch: 'main',                           // default: 'main'
    credentialsId: 'github-creds',            // optional
    checkoutTimeout: 10                       // default: 10 minutes
  ]
}
```

- Script Docker Compose pipeline (compose up with Git checkout):

```groovy
ScriptDockerComposePipeline() {
  projectName = 'my-app'                      // optional (compose project name)
  composeFile = 'docker-compose.yml'          // optional (default: docker-compose.yml)
  envVariables = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  agentLabel = 'docker-node'                  // optional agent label (default: 'any')
  gitConfig = [                               // optional Git configuration
    repoUrl: 'https://github.com/org/repo.git',
    branch: 'main',                           // default: 'main'
    credentialsId: 'github-creds',            // optional
    checkoutTimeout: 10                       // default: 10 minutes
  ]
}
```

- Global Docker pipeline (build + run):

```groovy
GlobalPipeline() {
  dockerImage = 'my-org/my-app:latest'         // required
  projectName = 'my-app'                      // required (container/service name)
  appPort = '8080'                            // optional
  networkName = 'my-network'                  // optional
  envFile = 'jenkins-secret-id'               // optional (credentialsId of a secret file)
  envVariables = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  buildArgs = [FOO: 'bar']                    // optional map of build args
  volumeDriver = 'my-volume-driver'           // optional volume driver for container (example: '/var/lib/docker/volumes:my-volume/_data' on host)
}
```

- Global Docker Compose pipeline (compose up):

```groovy
DockerComposePipeline() {
  projectName = 'my-app'                      // optional (compose project name)
  composeFile = 'docker-compose.yml'          // optional (default: docker-compose.yml)
  envVariables = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
}
```

- Kubernetes pipeline (build + deploy):

```groovy
KubePipeline() {
  dockerImage = 'my-org/my-app:latest'        // required
  projectName = 'my-app'                      // required (container/service name)
  appPort = '8080'                            // optional
  kubeNodePort = '30080'                      // optional nodePort for Service
  externalEndpointsIp = '10.0.0.50'           // MetalLB / loadBalancer IP(s)
  replicaCount = '3'                          // optional number of replicas (default: 1)
  healthCheckPath = '/health'                 // optional readiness probe path (default: '', which mean no health check)
  buildArgs = [FOO: 'bar']                    // optional map of build args
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
runningNewContainer('8080','my-app','localhost:32000/my-app:1.2.3','jenkins-secret-id','my-network',null,[FOO: 'bar'])
createDockerNetwork('my-network')
dockerComposeUp('docker-compose.yml','my-app',[FOO: 'bar'])
removingDanglingImage()
incrementMavenVersion()
```

Notes
- `vars/` functions remain the public API for Jenkinsfiles. Implementation details live under `src/com/nephren` and can be unit tested.
- The examples assume the shared library is registered in Jenkins as `nephren-shared-lib`. Adjust `@Library(...)` accordingly.

