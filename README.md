example of usage:

```bash
@Library('global-pipeline') _

GlobalPipeline() {
  dockerImage = <docker-image-build-result-name>
  projectName = <docker-projectOrContainer-name>
  appPort = <open-port> (optional)
  networkName = <docker-network-name> (optional)
  envFile = <jenkins-secret-file> (optional)
  buildArgs = [
      <args>: <value>,
      ...
  ] (optional)
}
```
