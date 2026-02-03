# 🚀 Nephren Shared Library

A Jenkins Shared Library providing reusable pipeline entrypoints and helper classes for Docker, Docker Compose, Kubernetes, and Maven workflows.

[![Jenkins](https://img.shields.io/badge/Jenkins-Shared%20Library-blue?logo=jenkins)](https://www.jenkins.io/doc/book/pipeline/shared-libraries/)

---

## 📁 Project Structure

```
nephren-shared-lib/
├── src/com/nephren/          # Helper classes (implementation details)
│   ├── DockerUtils.groovy
│   ├── DockerComposeUtils.groovy
│   ├── GitUtils.groovy
│   ├── KubernetesUtils.groovy
│   └── MavenVersioner.groovy
└── vars/                     # Public API (pipeline entrypoints)
    ├── DockerPipeline.groovy
    ├── ScriptDockerPipeline.groovy
    ├── DockerComposePipeline.groovy
    ├── ScriptDockerComposePipeline.groovy
    ├── KubePipeline.groovy
    ├── MavenIncrementVersionPipeline.groovy
    └── ... (helper functions)
```

---

## 🔧 Installation

Include the library in your Jenkinsfile (name depends on how you register it in Jenkins):

```groovy
@Library('nephren-shared-lib') _
```

---

## ⚙️ Pipeline Options (Applied to All Pipelines)

All pipelines come pre-configured with the following options:

| Option | Description | Default |
|--------|-------------|---------|
| `buildDiscarder` | Log rotation - keeps limited builds | 5 builds, 10MB file limit |
| `timeout` | Maximum pipeline execution time | 30 min (Docker), 45 min (K8s), 15 min (Maven) |
| `timestamps` | Adds timestamps to console output | ✅ Enabled |
| `skipDefaultCheckout` | Disables automatic checkout | ✅ Enabled |
| `disableConcurrentBuilds` | Prevents parallel builds of same job | ✅ Enabled |

---

## 📘 Pipeline Examples

### 🐳 Script Docker Pipeline

Build and deploy with explicit Git checkout configuration.

```groovy
ScriptDockerPipeline {
  dockerImage   = 'my-org/my-app:latest'       // required
  projectName   = 'my-app'                     // required (container/service name)
  appPort       = '8080'                       // optional
  networkName   = 'my-network'                 // optional
  envFile       = 'jenkins-secret-id'          // optional (credentialsId of a secret file)
  envVariables  = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  buildArgs     = [FOO: 'bar']                 // optional map of build args
  volumeDriver  = '/host/path:/container/path' // optional volume mount
  agentLabel    = 'docker-node'                // optional (default: 'any')
  gitConfig     = [                            // optional Git configuration
    repoUrl: 'https://github.com/org/repo.git',
    branch: 'main',                            // default: 'main'
    credentialsId: 'github-creds',             // optional
    checkoutTimeout: 10                        // default: 10 minutes
  ]
}
```

---

### 🐳 Script Docker Compose Pipeline

Compose up with explicit Git checkout configuration.

```groovy
ScriptDockerComposePipeline {
  projectName   = 'my-app'                     // optional (compose project name)
  composeFile   = 'docker-compose.yml'         // optional (default: docker-compose.yml)
  envVariables  = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  agentLabel    = 'docker-node'                // optional (default: 'any')
  gitConfig     = [                            // optional Git configuration
    repoUrl: 'https://github.com/org/repo.git',
    branch: 'main',                            // default: 'main'
    credentialsId: 'github-creds',             // optional
    checkoutTimeout: 10                        // default: 10 minutes
  ]
}
```

---

### 🐳 Docker Pipeline (SCM Checkout)

Build and run using Jenkins SCM configuration.

```groovy
DockerPipeline {
  dockerImage   = 'my-org/my-app:latest'       // required
  projectName   = 'my-app'                     // required (container/service name)
  appPort       = '8080'                       // optional
  networkName   = 'my-network'                 // optional
  envFile       = 'jenkins-secret-id'          // optional (credentialsId of a secret file)
  envVariables  = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
  buildArgs     = [FOO: 'bar']                 // optional map of build args
  volumeDriver  = '/host/path:/container/path' // optional volume mount
}
```

---

### 🐳 Docker Compose Pipeline (SCM Checkout)

Compose up using Jenkins SCM configuration.

```groovy
DockerComposePipeline {
  projectName   = 'my-app'                     // optional (compose project name)
  composeFile   = 'docker-compose.yml'         // optional (default: docker-compose.yml)
  envVariables  = [FOO: 'bar', BAZ: 'qux']     // optional map or list of KEY=VALUE strings
}
```

---

### ☸️ Kubernetes Pipeline

Build Docker image and deploy to Kubernetes cluster.

```groovy
KubePipeline {
  dockerImage        = 'my-org/my-app:latest'  // required
  projectName        = 'my-app'                // required (container/service name)
  appPort            = '8080'                  // optional
  kubeNodePort       = '30080'                 // optional nodePort for Service
  externalEndpointsIp = '10.0.0.50'            // MetalLB / loadBalancer IP(s)
  replicaCount       = '3'                     // optional (default: '1')
  healthCheckPath    = '/health'               // optional readiness probe path (default: '' = no health check)
  buildArgs          = [FOO: 'bar']            // optional map of build args
  kubectlPath        = '/snap/bin/microk8s kubectl' // optional (default: '/snap/bin/microk8s kubectl')
}
```

> **Note:** The `kubectlPath` parameter allows you to customize the kubectl binary location for different Kubernetes distributions (microk8s, k3s, standard kubectl, etc.)

---

### 📦 Maven Version Increment Pipeline

Automatically increment Maven project version and push to Git.

```groovy
MavenIncrementVersionPipeline {
  projectName = 'my-app'                       // required
  appType     = 'maven'                        // triggers Maven version increment when set
}
```

> **Note:** Set the `GIT_CREDENTIALS_ID` environment variable or configure the default credentials ID in `MavenVersioner.groovy` for Git push operations.

---

## 🛠️ Individual Helper Functions

Call helpers directly from a Jenkinsfile for custom pipelines:

```groovy
// Docker operations
buildDockerImage('localhost:32000/my-app:1.2.3', [ARG1: 'value'])
stoppingAndRemovingContainer('my-app')
runningNewContainer('8080', 'my-app', 'localhost:32000/my-app:1.2.3', 'jenkins-secret-id', 'my-network', null, [FOO: 'bar'])
createDockerNetwork('my-network')
removingDanglingImage()

// Docker Compose
dockerComposeUp('docker-compose.yml', 'my-app', [FOO: 'bar'])

// Kubernetes
prepareKubernetesDeployment('my-app', 'my-app:1.2.3', '8080', '10.0.0.50', '30080', '1', '/health')

// Git checkout
checkoutWithScm()
checkoutWithCredential(10, 'https://github.com/org/repo.git', 'main', 'github-creds')

// Maven
incrementMavenVersion()
incrementMavenVersion('custom-git-credentials-id')
```

---

## 📊 Parameter Reference

### Docker Pipelines

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `dockerImage` | String | ✅ | - | Docker image name with tag |
| `projectName` | String | ✅ | - | Container/service name |
| `appPort` | String | ❌ | - | Application port to expose |
| `networkName` | String | ❌ | - | Docker network name |
| `envFile` | String | ❌ | - | Jenkins credentials ID for env file |
| `envVariables` | Map/List | ❌ | - | Environment variables |
| `buildArgs` | Map | ❌ | `[:]` | Docker build arguments |
| `volumeDriver` | String | ❌ | - | Volume mount specification |
| `agentLabel` | String | ❌ | `'any'` | Jenkins agent label |

### Kubernetes Pipeline

| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `dockerImage` | String | ✅ | - | Docker image name with tag |
| `projectName` | String | ✅ | - | Deployment/service name |
| `appPort` | String | ❌ | - | Container port |
| `kubeNodePort` | String | ❌ | - | NodePort for service |
| `externalEndpointsIp` | String | ❌ | - | MetalLB/LoadBalancer IP |
| `replicaCount` | String | ❌ | `'1'` | Number of replicas |
| `healthCheckPath` | String | ❌ | `''` | Readiness/liveness probe path |
| `buildArgs` | Map | ❌ | `[:]` | Docker build arguments |
| `kubectlPath` | String | ❌ | `'/snap/bin/microk8s kubectl'` | Path to kubectl binary |

---

## 📝 Notes

- `vars/` functions are the public API for Jenkinsfiles
- Implementation details in `src/com/nephren/` can be unit tested
- Examples assume the library is registered as `nephren-shared-lib` — adjust `@Library(...)` accordingly
- All pipelines include workspace cleanup in the `post.always` block

---

## 📄 License

MIT License - Feel free to use and modify as needed.

