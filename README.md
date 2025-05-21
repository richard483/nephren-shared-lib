example of usage:

```bash
@Library('global-pipeline') _

# docker pipeline
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

# kubernetes pipeline
KubePipeline() {
	dockerImage = <docker-image-build-result-name>
	projectName = <kubernetes-serviceOrProject-name>
	appPort = <open-port>
    externalEndpointsIp = <external-endpoints-ip> # 10.10.10.0/24
}
```
